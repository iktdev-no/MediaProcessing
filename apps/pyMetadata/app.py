import logging
import signal
import sys
import os
from typing import List, Optional
import uuid
import threading
import json
import time
from .clazz.shared import ConsumerRecord, MediaEvent, decode_key, decode_value, suppress_ignore, consume_on_key
from fuzzywuzzy import fuzz

from .algo.AdvancedMatcher import AdvancedMatcher
from .algo.SimpleMatcher import SimpleMatcher
from .algo.PrefixMatcher import PrefixMatcher
from .clazz.KafkaMessageSchema import KafkaMessage, MessageDataWrapper
from .clazz.Metadata import Metadata
from kafka import KafkaConsumer, KafkaProducer

from .sources.anii import Anii
from .sources.imdb import Imdb
from .sources.mal import Mal

# Konfigurer Kafka-forbindelsen
bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVER") or "127.0.0.1:9092"
consumer_group = os.environ.get("KAFKA_CONSUMER_ID") or f"MetadataConsumer"
kafka_topic = os.environ.get("KAFKA_TOPIC") or "mediaEvents"


# Konfigurer logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


# Kafka consumer-klasse
class KafkaConsumerThread(threading.Thread):
    def __init__(self, bootstrap_servers, topic, consumer_group):
        super().__init__()
        self.bootstrap_servers = bootstrap_servers
        self.consumer_group = consumer_group
        self.topic = topic
        self.shutdown = threading.Event()

    def run(self):
        consumer = None
        try:
            consumer = KafkaConsumer(
                self.topic,
                bootstrap_servers=self.bootstrap_servers,
                group_id=self.consumer_group,
                key_deserializer=lambda x: decode_key(x),
                value_deserializer=lambda x: decode_value(x)
            )
            logger.info("Kafka Consumer started")

        except:
            logger.exception("Kafka Consumer failed to start")
            self.stop()
            #sys.exit(1)


        while not self.shutdown.is_set():
            for cm in consumer:
                if self.shutdown.is_set():
                    break
                message: ConsumerRecord = ConsumerRecord(cm)  
                

                # Sjekk om meldingen har målnøkkelen
                if message.key in consume_on_key:
                    logger.info("==> Incoming message: %s \n%s", message.key, message.value)
                    # Opprett en ny tråd for å håndtere meldingen
                    handler_thread = MessageHandlerThread(message)
                    handler_thread.start()
                else:
                    if (message.key not in suppress_ignore):
                        logger.debug("Ignored message: key=%s", message.key)
            # Introduce a small sleep to reduce CPU usage
            time.sleep(1)
        if consumer is not None:
            consumer.close()
            logger.info("Kafka Consumer stopped")

    def stop(self):
        self.shutdown.set()
        global should_stop 
        should_stop = True

# Kafka message handler-klasse
class MessageHandlerThread(threading.Thread):
    producerMessageKey = "event:media-metadata-search:performed"
    def __init__(self, message: ConsumerRecord):
        super().__init__()
        self.message = message

    def run(self):

        mediaEvent = MediaEvent(message=self.message)

        if mediaEvent.data is None:
            logger.error("No data present for %s", self.message.value)
            return
        if mediaEvent.isConsumable() == False:
            logger.info("Message status is not of 'COMPLETED', %s", self.message.value)
            return
    
        logger.info("Processing record: key=%s, value=%s", self.message.key, self.message.value)


        searchableTitles: List[str] = mediaEvent.data["searchTitles"]
        searchableTitles.extend([
            mediaEvent.data["title"],
            mediaEvent.data["sanitizedName"]
        ])


        joinedTitles = ", ".join(searchableTitles)
        logger.info("Searching for %s", joinedTitles)
        result: Metadata | None = self.__getMetadata(searchableTitles)

        result_message: str | None = None
        if (result is None):
            result_message = f"No result for {joinedTitles}"
            logger.info(result_message)

        messageData = MessageDataWrapper(
            status =  "ERROR" if result is None else "COMPLETED",
            message =  result_message,
            data = result,
            derivedFromEventId = mediaEvent.eventId
        )

        producerMessage = KafkaMessage(referenceId=mediaEvent.referenceId, data=messageData).to_json()

        # Serialiser resultatet til JSON som strenger
        result_json = json.dumps(producerMessage)

        logger.info("<== Outgoing message: %s \n%s", self.producerMessageKey, result_json)

        # Send resultatet tilbake ved hjelp av Kafka-producer
        producer = KafkaProducer(
            bootstrap_servers=bootstrap_servers,
            key_serializer=lambda k: k.encode('utf-8') if isinstance(k, str) else None,
            value_serializer=lambda v: v.encode('utf-8') if isinstance(v, str) else None
        )
        producer.send(kafka_topic, key=self.producerMessageKey, value=result_json)
        producer.close()




    def __getMetadata(self, titles: List[str]) -> Metadata | None:
        mal = Mal(titles=titles)
        anii = Anii(titles=titles)
        imdb = Imdb(titles=titles)

        results: List[Metadata] = [
            mal.search(),
            anii.search(),
            imdb.search()
        ]
        filtered_results = [result for result in results if result is not None]
        logger.info("Simple matcher")
        simpleSelector = SimpleMatcher(titles=titles, metadata=filtered_results).getBestMatch()
        logger.info("Advanced matcher")
        advancedSelector = AdvancedMatcher(titles=titles, metadata=filtered_results).getBestMatch()
        logger.info("Prefrix matcher")
        prefixSelector = PrefixMatcher(titles=titles, metadata=filtered_results).getBestMatch()
        if simpleSelector is not None:
            return simpleSelector
        if advancedSelector is not None:
            return advancedSelector
        if prefixSelector is not None:
            return prefixSelector
        return None


# Global variabel for å indikere om applikasjonen skal avsluttes
should_stop = False

# Signalhåndteringsfunksjon
def signal_handler(sig, frame):
    global should_stop
    should_stop = True

# Hovedprogrammet
def main():
    try:
        # Angi signalhåndterer for å fange opp SIGINT (Ctrl+C)
        signal.signal(signal.SIGINT, signal_handler)

        # Opprett og start consumer-tråden
        consumer_thread = KafkaConsumerThread(bootstrap_servers, kafka_topic, consumer_group)
        consumer_thread.start()

        logger.info("App started")

        # Vent til should_stop er satt til True for å avslutte applikasjonen
        while not should_stop:
            time.sleep(60)

        # Stopp consumer-tråden
        consumer_thread.stop()
        consumer_thread.join()
    except:
        logger.info("App crashed")
        sys.exit(1)

    logger.info("App stopped")
    sys.exit(0)
if __name__ == '__main__':
    main()

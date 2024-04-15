import logging
import signal
import sys
import os
from typing import List, Optional
import uuid
import threading
import json
import time

from kafka import KafkaConsumer, KafkaProducer
from fuzzywuzzy import fuzz
from sources.result import DataResult, Metadata, NamedDataResult
from sources.anii import metadata as AniiMetadata
from sources.imdb import metadata as ImdbMetadata
from sources.mal import metadata as MalMetadata
from sources.cache import ResultCache
from sources.select import UseSource

# Konfigurer Kafka-forbindelsen
bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVER") or "127.0.0.1:9092"
consumer_group = os.environ.get("KAFKA_CONSUMER_ID") or f"MetadataConsumer"
kafka_topic = os.environ.get("KAFKA_TOPIC") or "mediaEvents"


suppress_ignore: List[str] = [
    "event:media-process:started",
    "event:request-process:started",
    "event::save",
    "event:media-process:completed",
    "event:work-encode:created",
    "event:work-extract:created",
    "event:work-convert:created",
    "event:work-encode:performed",
    "event:work-extract:performed",
    "event:work-convert:performed",    
    "event:media-read-out-cover:performed",
    "event:work-download-cover:performed",
    "event:media-read-out-name-and-type:performed",
    "event:media-parse-stream:performed",
    "event:media-extract-parameter:created",
    "event:media-encode-parameter:created",
    "event:media-metadata-search:performed"
]

# Konfigurer logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class ProducerDataValueSchema:
    def __init__(self, referenceId, data: DataResult):
        self.referenceId = referenceId
        self.data = data

    def to_dict(self):
        return {
            'referenceId': self.referenceId,
            'eventId': str(uuid.uuid4()),
            'data': self.data.to_dict() if self.data else None
        }

    def to_json(self):
        data_dict = self.to_dict()
        return json.dumps(data_dict)

def decode_key(key_bytes):
    return key_bytes.decode('utf-8') if key_bytes else None

def decode_value(value_bytes):
    return json.loads(value_bytes.decode('utf-8')) if value_bytes else None


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
            for message in consumer:
                if self.shutdown.is_set():
                    break

                

                # Sjekk om meldingen har målnøkkelen
                if message.key == "request:metadata:obtain" or message.key == "event:media-read-base-info:performed":
                    logger.info("==> Incoming message: %s \n%s", message.key, message.value)
                    # Opprett en ny tråd for å håndtere meldingen
                    handler_thread = MessageHandlerThread(message)
                    handler_thread.start()
                else:
                    if (message.key not in suppress_ignore):
                        logger.info("Ignored message: key=%s", message.key)
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
    def __init__(self, message):
        super().__init__()
        self.message = message

    def run(self):
        logger.info("Handling message: key=%s, value=%s", self.message.key, self.message.value)
        if 'data' not in self.message.value:
            logger.error("data is not present in message!")
        messageData = self.message.value["data"]
        # Sjekk om meldingen har en Status
        if 'status' in messageData:
            status_type = messageData['status']

            # Sjekk om statusen er COMPLETED
            if status_type == 'COMPLETED':
                baseName = messageData["sanitizedName"]
                title = messageData["title"]

                eventId = self.message.value["eventId"]

                logger.info("Searching for %s", title)
                result = self.get_metadata(title, baseName, eventId)
                if (result is None):
                    logger.info("No result for %s or %s", title, baseName)

                producerMessage = self.compose_message(referenceId=self.message.value["referenceId"], result=result)

                # Serialiser resultatet til JSON som strenger
                result_json = json.dumps(producerMessage.to_dict())

                # Send resultatet tilbake ved hjelp av Kafka-producer
                producer = KafkaProducer(
                    bootstrap_servers=bootstrap_servers,
                    key_serializer=lambda k: k.encode('utf-8') if isinstance(k, str) else None,
                    value_serializer=lambda v: v.encode('utf-8') if isinstance(v, str) else None
                )
                producer.send(kafka_topic, key="event:media-metadata-search:performed", value=result_json)
                producer.close()
            else:
                logger.info("Message status is not of 'COMPLETED', %s", self.message.value)
        else:
            logger.warn("No status present for %s", self.message.value)

    def get_metadata(self, name: str, baseName: str, evnetId: str) -> Optional[DataResult]:
        result = None
        logger.info("Checking cache")
        titleCache = ResultCache.get(name)
        if (titleCache is None):
            titleCache = UseSource(title=name, eventId=evnetId).select_result()
            if titleCache is not None:
                logger.info("Storing response for %s in in-memory cache", name)
                ResultCache.add(title=name, result=titleCache)
        else:
            logger.info("Cache hit for %s", name)                

        baseNameCache = ResultCache.get(baseName)
        if (baseNameCache is None):
            baseNameCache = UseSource(title=baseName, eventId=evnetId).select_result()
            if baseNameCache is not None:
                logger.info("Storing response for %s in in-memory cache", baseName)
                ResultCache.add(title=baseName, result=baseNameCache)
        else:
            logger.info("Cache hit for %s", baseName) 

        if titleCache is not None and baseNameCache is not None:
            if (titleCache.data.type.lower() == "movie" or baseNameCache.data.type.lower() == "movie"):
                result = baseNameCache
            else:
                result = titleCache
        elif titleCache is not None:
            result = titleCache
        elif baseNameCache is not None:
            result = baseNameCache

        return result


    def compose_message(self, referenceId: str, result: DataResult) -> ProducerDataValueSchema:
        return ProducerDataValueSchema(
            referenceId=referenceId,
            data=result
        )


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

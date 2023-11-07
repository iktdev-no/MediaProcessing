import logging
import signal
import sys
import os
from typing import Optional
import uuid
import threading
import json
import time
from kafka import KafkaConsumer, KafkaProducer
from fuzzywuzzy import fuzz
from sources.result import DataResult, Metadata
from sources.anii import metadata as AniiMetadata
from sources.imdb import metadata as ImdbMetadata
from sources.mal import metadata as MalMetadata
from sources.cache import ResultCache
from sources.select import UseSource

# Konfigurer Kafka-forbindelsen
bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVER") or "127.0.0.1:9092"
consumer_group = os.environ.get("KAFKA_CONSUMER_ID") or f"Metadata-{uuid.uuid4()}"
kafka_topic = os.environ.get("KAFKA_TOPIC") or "127.0.0.1:9092"

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
    def __init__(self, referenceId, statusType, errorMessage, data):
        self.referenceId = referenceId
        self.statusType = statusType
        self.errorMessage = errorMessage
        self.data = data

    def to_dict(self):
        return {
            'referenceId': self.referenceId,
            'status': {
                'statusType': self.statusType,
                'errorMessage': self.errorMessage
            },
            'data': self.data.to_dict() if self.data else None
        }

    def to_json(self):
        data_dict = self.to_dict()
        return json.dumps(data_dict)

    @classmethod
    def from_dict(cls, data_dict):
        referenceId = data_dict.get('referenceId')
        statusType = data_dict['status'].get('statusType')
        errorMessage = data_dict['status'].get('errorMessage')
        data = data_dict.get('data')

        return cls(referenceId, statusType, errorMessage, data)

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
            sys.exit(1)


        while not self.shutdown.is_set():
            for message in consumer:
                if self.shutdown.is_set():
                    break

                

                # Sjekk om meldingen har målnøkkelen
                if message.key == "request:metadata:obtain" or message.key == "event:reader:received-file":
                    logger.info("Received message: key=%s, value=%s", message.key, message.value)
                    # Opprett en ny tråd for å håndtere meldingen
                    handler_thread = MessageHandlerThread(message)
                    handler_thread.start()
                else:
                    logger.info("Ignorert message: key=%s", message.key)
            # Introduce a small sleep to reduce CPU usage
            time.sleep(1)

        consumer.close()
        logger.info("Kafka Consumer stopped")

    def stop(self):
        self.shutdown.set()

# Kafka message handler-klasse
class MessageHandlerThread(threading.Thread):
    def __init__(self, message):
        super().__init__()
        self.message = message

    def run(self):
        logger.info("Handling message: key=%s, value=%s", self.message.key, self.message.value)

        # Sjekk om meldingen har en Status
        if 'status' in self.message.value:
            status_type = self.message.value['status']['statusType']

            # Sjekk om statusen er SUCCESS
            if status_type == 'SUCCESS':
                baseName = self.message.value["data"]["sanitizedName"]
                title = self.message.value['data']["title"]

                result = self.get_metadata(title)
                if (result is None):
                    result = self.get_metadata(baseName)

                producerMessage = self.compose_message(referenceId=self.message.value["referenceId"], result=result)

                # Serialiser resultatet til JSON som strenger
                result_json = json.dumps(producerMessage.to_dict())

                # Send resultatet tilbake ved hjelp av Kafka-producer
                producer = KafkaProducer(
                    bootstrap_servers=bootstrap_servers,
                    key_serializer=lambda k: k.encode('utf-8') if isinstance(k, str) else None,
                    value_serializer=lambda v: v.encode('utf-8') if isinstance(v, str) else None
                )
                producer.send(kafka_topic, key="event:metadata:obtained", value=result_json)
                producer.close()

    def get_metadata(self, name: str) -> Optional[DataResult]:
        result = None
        logger.info("Checking cache for offloading")
        cache_result = ResultCache.get(name)
        if cache_result:
            logger.info("Cache hit for %s", name)
            result = cache_result
        else:
            logger.info("Not in cache: %s", name)
            logger.info("Searching in sources for information about %s", name)
            result: Optional[DataResult] = UseSource(title=name).select_result()
            if (result.statusType == "SUCCESS"):
                logger.info("Storing response for %s in in-memory cache", name)
                ResultCache.add(name, result)
        return result


    def compose_message(self, referenceId: str, result: DataResult) -> ProducerDataValueSchema:
        return ProducerDataValueSchema(
            referenceId=referenceId,
            statusType=result.statusType,
            errorMessage=result.errorMessage,
            data=result.data
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

if __name__ == '__main__':
    main()

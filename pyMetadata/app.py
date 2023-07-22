import logging
import signal
import sys
import os
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
        consumer = KafkaConsumer(
            self.topic,
            bootstrap_servers=self.bootstrap_servers,
            group_id=self.consumer_group,
            key_deserializer=lambda x: decode_key(x),
            value_deserializer=lambda x: decode_value(x)
        )

        logger.info("Kafka Consumer started")

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
                data_value = self.message.value['data']["title"]

                result = None # Will be assigned by either cache_result or sel.perform_action
                logger.info("Checking cache for offloading")
                cache_result = ResultCache.get(data_value)
                if cache_result:
                    logger.info("Cache hit for %s", data_value)
                    result = cache_result
                else:
                    logger.info("Not in cache: %s", data_value)
                    logger.info("Searching in sources for information about %s", data_value)
                    result = self.perform_action(title=data_value)
                    if (result.statusType == "SUCCESS"):
                        logger.info("Storing response for %s in in-memory cache", data_value)
                        ResultCache.add(data_value, result)


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



    def perform_action(self, title) -> DataResult:
        anii = AniiMetadata(title)
        imdb = ImdbMetadata(title)
        mal = MalMetadata(title)

        mal_result = mal.lookup()
        anii_result = anii.lookup()
        imdb_result = imdb.lookup()

        # Sammenlign resultater basert på likheter og sammenhenger med tittelen
        if anii_result.statusType == "SUCCESS" and imdb_result.statusType == "SUCCESS" and mal_result.statusType == "SUCCESS":
            # Begge registrene ga suksessresultater, bruk fuzzy matching for å gjøre en vurdering
            title_similarity_anii = fuzz.ratio(title.lower(), anii_result.data.title.lower())
            title_similarity_imdb = fuzz.ratio(title.lower(), imdb_result.data.title.lower())
            title_similarity_mal = fuzz.ratio(title.lower(), mal_result.data.title.lower())

            alt_titles_anii = anii_result.data.altTitle
            alt_titles_imdb = imdb_result.data.altTitle
            alt_titles_mal = mal_result.data.altTitle

            # Sammenlign likheter mellom tittel og registertitler, inkludert alternative titler
            if (
                title_similarity_anii * 0.8 + sum(fuzz.ratio(title.lower(), alt_title.lower()) for alt_title in alt_titles_anii) * 0.2
                < title_similarity_mal * 0.8 + sum(fuzz.ratio(title.lower(), alt_title.lower()) for alt_title in alt_titles_mal) * 0.2
            ):
                most_likely_result = mal_result
            elif (
                title_similarity_imdb * 0.8 + sum(fuzz.ratio(title.lower(), alt_title.lower()) for alt_title in alt_titles_imdb) * 0.2
                > title_similarity_anii * 0.8 + sum(fuzz.ratio(title.lower(), alt_title.lower()) for alt_title in alt_titles_anii) * 0.2
            ):
                most_likely_result = imdb_result
            else:
                most_likely_result = anii_result

        elif anii_result.statusType == "SUCCESS":
            # AniList ga suksessresultat, bruk det som det mest sannsynlige
            most_likely_result = anii_result

        elif imdb_result.statusType == "SUCCESS":
            # IMDb ga suksessresultat, bruk det som det mest sannsynlige
            most_likely_result = imdb_result

        elif mal_result.statusType == "SUCCESS":
            # MAL ga suksessresultat, bruk det som det mest sannsynlige
            most_likely_result = mal_result

        else:
            # Ingen resultater, bruk AniList hvis tilgjengelig
            most_likely_result = anii_result

        # Returner det mest sannsynlige resultatet
        return most_likely_result



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
    # Angi signalhåndterer for å fange opp SIGINT (Ctrl+C)
    signal.signal(signal.SIGINT, signal_handler)

    # Opprett og start consumer-tråden
    consumer_thread = KafkaConsumerThread(bootstrap_servers, kafka_topic, consumer_group)
    consumer_thread.start()

    logger.info("App started")

    # Vent til should_stop er satt til True for å avslutte applikasjonen
    while not should_stop:
        pass

    # Stopp consumer-tråden
    consumer_thread.stop()
    consumer_thread.join()

    logger.info("App stopped")

if __name__ == '__main__':
    main()

import signal
import sys, os, uuid
import threading
import json
from kafka import KafkaConsumer, KafkaProducer
from fuzzywuzzy import fuzz
from sources.result import Result, Metadata
from sources.anii import metadata as AniiMetadata
from sources.imdb import metadata as ImdbMetadata

# Konfigurer Kafka-forbindelsen
bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVER") if os.environ.get("KAFKA_BOOTSTRAP_SERVER") != None else "127.0.0.1:9092"
consumer_group = os.environ.get("KAFKA_CONSUMER_ID") if os.environ.get("KAFKA_CONSUMER_ID") != None else f"Metadata-{uuid.uuid4()}"
kafka_topic = os.environ.get("KAFKA_BOOTSTRAP_SERVER") if os.environ.get("KAFKA_BOOTSTRAP_SERVER") != None else "127.0.0.1:9092"

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



# Kafka consumer-klasse
class KafkaConsumerThread(threading.Thread):
    def __init__(self, bootstrap_servers, topic):
        super().__init__()
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.shutdown = threading.Event()

    def run(self):
        consumer = KafkaConsumer(self.topic, bootstrap_servers=self.bootstrap_servers)

        while not self.shutdown.is_set():
            for message in consumer:
                if self.shutdown.is_set():
                    break

                # Sjekk om meldingen har målnøkkelen
                if message.key == "request:metadata:obtain" or message.key == "event:reader:received-file":
                    # Opprett en ny tråd for å håndtere meldingen
                    handler_thread = MessageHandlerThread(message)
                    handler_thread.start()

        consumer.close()

    def stop(self):
        self.shutdown.set()

# Kafka message handler-klasse
class MessageHandlerThread(threading.Thread):
    def __init__(self, message):
        super().__init__()
        self.message = message

    def run(self):
        # Deserialiser meldingsverdien fra JSON til et Python-dictionary
        message_value = json.loads(self.message.value)

        # Sjekk om meldingen har en Status
        if 'status' in message_value:
            status_type = message_value['status']['statusType']

            # Sjekk om statusen er SUCCESS
            if status_type == 'SUCCESS':
                data_value = message_value['data']["title"]

                # Utfør handlingen basert på verdien
                result = self.perform_action(title=data_value)

                producerMessage = self.compose_message(referenceId=message_value["referenceId"], result=result)

                # Serialiser resultatet til JSON
                result_json = json.dumps(producerMessage.to_json())

                # Send resultatet tilbake ved hjelp av Kafka-producer
                producer = KafkaProducer(bootstrap_servers=bootstrap_servers)
                producer.send(kafka_topic, key="event:metadata:obtained", value=result_json)
                producer.close()

    def perform_action(self, title) -> Result:
        anii = AniiMetadata(title)
        imdb = ImdbMetadata(title)

        anii_result = anii.lookup()
        imdb_result = imdb.lookup()

        # Sammenlign resultater basert på likheter og sammenhenger med tittelen
        if anii_result.statusType == "SUCCESS" and imdb_result.statusType == "SUCCESS":
            # Begge registrene ga suksessresultater, bruk fuzzy matching for å gjøre en vurdering
            title_similarity_anii = fuzz.ratio(title.lower(), anii_result.data.title.lower())
            title_similarity_imdb = fuzz.ratio(title.lower(), imdb_result.data.title.lower())

            # Sammenlign likheter mellom tittel og registertitler
            if title_similarity_anii > title_similarity_imdb:
                most_likely_result = anii_result
            else:
                most_likely_result = imdb_result

        elif anii_result.statusType == "SUCCESS":
            # AniList ga suksessresultat, bruk det som det mest sannsynlige
            most_likely_result = anii_result

        elif imdb_result.statusType == "SUCCESS":
            # IMDb ga suksessresultat, bruk det som det mest sannsynlige
            most_likely_result = imdb_result

        else:
            # Begge registrene feilet, håndter etter eget behov
            most_likely_result = Result(statusType="ERROR", errorMessage="No Result")

        # Returner det mest sannsynlige resultatet
        return most_likely_result

    
    def compose_message(self, referenceId: str, result: Result) -> ProducerDataValueSchema:
        """"""
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
    consumer_thread = KafkaConsumerThread(bootstrap_servers, kafka_topic)
    consumer_thread.start()

    # Vent til should_stop er satt til True for å avslutte applikasjonen
    while not should_stop:
        pass

    # Stopp consumer-tråden
    consumer_thread.stop()
    consumer_thread.join()

if __name__ == '__main__':
    main()
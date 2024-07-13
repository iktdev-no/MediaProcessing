import logging
import signal
import sys
import os
from typing import List, Optional
import uuid
import threading
import json
import time
from fuzzywuzzy import fuzz
import mysql.connector
from datetime import datetime

from algo.AdvancedMatcher import AdvancedMatcher
from algo.SimpleMatcher import SimpleMatcher
from algo.PrefixMatcher import PrefixMatcher
from clazz.shared import EventMetadata, MediaEvent, event_data_to_json, json_to_media_event
from clazz.KafkaMessageSchema import KafkaMessage, MessageDataWrapper
from clazz.Metadata import Metadata
from kafka import KafkaConsumer, KafkaProducer

from sources.anii import Anii
from sources.imdb import Imdb
from sources.mal import Mal




# Konfigurer Kafka-forbindelsen
bootstrap_servers = os.environ.get("KAFKA_BOOTSTRAP_SERVER") or "127.0.0.1:9092"
consumer_group = os.environ.get("KAFKA_CONSUMER_ID") or f"MetadataConsumer"
kafka_topic = os.environ.get("KAFKA_TOPIC") or "mediaEvents"

events_server_address = os.environ.get("DATABASE_ADDRESS") or "127.0.0.1"
events_server_port  = os.environ.get("DATABASE_PORT") or "3306"
events_server_database_name = os.environ.get("DATABASE_NAME_E") or "events"
events_server_username = os.environ.get("DATABASE_USERNAME") or "root"
events_server_password = os.environ.get("DATABASE_PASSWORD") or "root"




# Konfigurer logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class EventsPullerThread(threading.Thread):
    connector = None
    def __init__(self):
        super().__init__()
        self.shutdown = threading.Event()

    def run(self) -> None:
        while not self.shutdown.is_set():
            connection = None
            cursor = None
            try:
                connection = mysql.connector.connect(
                    host=events_server_address,
                    port=events_server_port,
                    database=events_server_database_name,
                    user=events_server_username,
                    password=events_server_password
                )
                cursor = connection.cursor(dictionary=True)
                cursor.execute("""
                                    SELECT e1.*
                                    FROM events e1
                                    LEFT JOIN events e2 
                                        ON e1.referenceId = e2.referenceId 
                                        AND e2.event = 'event:media-metadata-search:performed'
                                    WHERE e1.event = 'event:media-read-base-info:performed'
                                    AND e2.referenceId IS NULL;
                               """)
                # not event:media-metadata-search:performed
                for row in cursor.fetchall():
                    if self.shutdown.is_set():
                        break
                    handler_thread = MessageHandlerThread(row)
                    handler_thread.start()

                # Introduce a small sleep to reduce CPU usage
                time.sleep(5000)
            except mysql.connector.Error as err:
                logger.error("Database error: %s", err)
            finally:
                if cursor:
                    cursor.close()
                if connection:
                    connection.close()

    def stop(self):
        self.shutdown.set()
        global should_stop 
        should_stop = True

# Kafka message handler-klasse
class MessageHandlerThread(threading.Thread):
    mediaEvent: MediaEvent|None = None
    def __init__(self, row):
        super().__init__()
        self.mediaEvent = json_to_media_event(json.loads(row['data']))

    def run(self):
        if (self.mediaEvent is None):
            logger.error("Event does not contain anything...")
            return

        event: MediaEvent = self.mediaEvent
    
        logger.info("Processing event: event=%s, value=%s", event.eventType, event)


        searchableTitles: List[str] = event.data.searchTitles
        searchableTitles.extend([
            event.data.title,
            event.data.sanitizedName
        ])


        joinedTitles = ", ".join(searchableTitles)
        logger.info("Searching for %s", joinedTitles)
        result: Metadata | None = self.__getMetadata(searchableTitles)

        result_message: str | None = None
        if (result is None):
            result_message = f"No result for {joinedTitles}"
            logger.info(result_message)


        producedEvent = MediaEvent(
            metadata = EventMetadata(
                referenceId=event.metadata.referenceId,
                eventId=str(uuid.uuid4()),
                derivedFromEventId=event.metadata.eventId,
                status= "Failed" if result is None else "Success",
                created= datetime.now().isoformat()
            ),
            data=result,
            eventType="event:media-metadata-search:performed"
        )

        
        logger.info("<== Outgoing message: %s \n%s", event.eventType, event_data_to_json(producedEvent))
        self.insert_into_database(producedEvent)



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
    
    def insert_into_database(self, event: MediaEvent):
        try:
            connection = mysql.connector.connect(
                host=events_server_address,
                port=events_server_port,
                database=events_server_database_name,
                user=events_server_username,
                password=events_server_password
            )
            cursor = connection.cursor()

            query = """
                INSERT INTO events (referenceId, eventId, event, data)
                VALUES (%s, %s, %s, %s)
            """
            cursor.execute(query, (
                event.metadata.referenceId,
                event.metadata.eventId,
                event.eventType,
                event_data_to_json(event)
            ))
            connection.commit()
            cursor.close()
            connection.close()
            logger.info("Storing event")
        except mysql.connector.Error as err:
            logger.error("Error inserting into database: %s", err)    


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
        consumer_thread = EventsPullerThread()
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

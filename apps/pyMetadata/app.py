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
import asyncio

import mysql.connector.cursor

from algo.AdvancedMatcher import AdvancedMatcher
from algo.SimpleMatcher import SimpleMatcher
from algo.PrefixMatcher import PrefixMatcher
from clazz.shared import EventMetadata, MediaEvent, event_data_to_json, json_to_media_event
from clazz.Metadata import Metadata

from sources.anii import Anii
from sources.imdb import Imdb
from sources.mal import Mal

from mysql.connector.abstracts import MySQLConnectionAbstract
from mysql.connector.pooling import PooledMySQLConnection
from mysql.connector.types import RowType as MySqlRowType


# Konfigurer Database
events_server_address = os.environ.get("DATABASE_ADDRESS") or "192.168.2.250" # "127.0.0.1"
events_server_port  = os.environ.get("DATABASE_PORT") or "3306"
events_server_database_name = os.environ.get("DATABASE_NAME_E") or "eventsV3" # "events"
events_server_username = os.environ.get("DATABASE_USERNAME") or "root"
events_server_password = os.environ.get("DATABASE_PASSWORD") or "shFZ27eL2x2NoxyEDBMfDWkvFO"  #"root" // default password




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
    def __init__(self):
        super().__init__()
        self.shutdown = threading.Event()
    
    def getEventsAvailable(self, connection: PooledMySQLConnection | MySQLConnectionAbstract) -> List[MySqlRowType]:
        cursor = connection.cursor(dictionary=True)
        cursor.execute("""
                            SELECT *
                            FROM events
                            WHERE referenceId IN (
                                SELECT referenceId
                                FROM events
                                GROUP BY referenceId
                                HAVING 
                                    SUM(event = 'event:media-read-base-info:performed') > 0
                                    AND SUM(event = 'event:media-metadata-search:performed') = 0
                                    AND SUM(event = 'event:media-process:completed') = 0
                            )
                            AND event = 'event:media-read-base-info:performed';
        """)
        row = cursor.fetchall()
        cursor.close()
        return row
        
    def storeProducedEvent(self, connection: PooledMySQLConnection | MySQLConnectionAbstract, event: MediaEvent) -> bool:
        try:
            cursor = connection.cursor()

            query = """
                INSERT INTO events (referenceId, eventId, event, data)
                VALUES (%s, %s, %s, %s)
            """
            cursor.execute(query, (
                event.metadata.referenceId,
                event.metadata.eventId,
                "event:media-metadata-search:performed",
                event_data_to_json(event)
            ))
            connection.commit()
            cursor.close()
            return True
        except mysql.connector.Error as err:
            logger.error("Error inserting into database: %s", err) 
            return False           

    def run(self) -> None:
        logger.info(f"Using {events_server_address}:{events_server_port} on table: {events_server_database_name}")
        while not self.shutdown.is_set():
            producedMessage: bool = False

            connection = mysql.connector.connect(
                    host=events_server_address,
                    port=events_server_port,
                    database=events_server_database_name,
                    user=events_server_username,
                    password=events_server_password
            )
            try:
                rows = self.getEventsAvailable(connection=connection)
                for row in rows:
                    if (row is not None):
                        try:
                            referenceId = row["referenceId"]
                            incomingEventType = row["event"]
                            logMessage = f"""
============================================================================
Found message
{referenceId} 
{incomingEventType}
============================================================================\n"""
                            logger.info(logMessage)
                            
                            event: MediaEvent = json_to_media_event(row["data"])
                            producedEvent = asyncio.run(MetadataEventHandler(event).run())

                            producedMessage = f"""
============================================================================
Producing message
{referenceId}
{incomingEventType}

{event_data_to_json(producedEvent)}
============================================================================\n"""
                            logger.info(producedMessage)
                            
                            producedEvent = self.storeProducedEvent(connection=connection, event=producedEvent)
                            
                        except Exception as e:
                            """Produce failure here"""
                            logger.exception(e)
                            producedEvent = MediaEvent(
                                metadata = EventMetadata(
                                    referenceId=event.metadata.referenceId,
                                    eventId=str(uuid.uuid4()),
                                    derivedFromEventId=event.metadata.eventId,
                                    status= "Failed",
                                    created= datetime.now().isoformat(),
                                    source="metadataApp"
                                ),
                                data=None,
                                eventType="EventMediaMetadataSearchPerformed"
                            )
                            self.storeProducedEvent(connection=connection, event=producedEvent)
                
            except mysql.connector.Error as err:
                logger.error("Database error: %s", err)
            finally:
                if connection:
                    connection.close()
                    connection = None
            # Introduce a small sleep to reduce CPU usage
            time.sleep(2)
        if (self.shutdown.is_set()):
            logger.info("Shutdown is set..")




    def stop(self):
        self.shutdown.set()
        global should_stop 
        should_stop = True

class MetadataEventHandler:
    mediaEvent: MediaEvent | None = None

    def __init__(self, data: MediaEvent):
        super().__init__()
        self.mediaEvent = data
        logger.info(self.mediaEvent)

    async def run(self) -> MediaEvent | None:
        logger.info("Starting search")
        if self.mediaEvent is None:
            logger.error("Event does not contain anything...")
            return None

        event: MediaEvent = self.mediaEvent

        searchableTitles: List[str] = event.data.searchTitles
        searchableTitles.extend([
            event.data.title,
            event.data.sanitizedName
        ])

        joinedTitles = "\n".join(searchableTitles)
        logger.info("Searching for: %s", joinedTitles)

        # Kjør den asynkrone søkemetoden
        result: Metadata | None = await self.__getMetadata(searchableTitles)

        result_message: str | None = None
        if result is None:
            result_message = f"No result for {joinedTitles}"
            logger.info(result_message)

        producedEvent = MediaEvent(
            metadata=EventMetadata(
                referenceId=event.metadata.referenceId,
                eventId=str(uuid.uuid4()),
                derivedFromEventId=event.metadata.eventId,
                status="Failed" if result is None else "Success",
                created=datetime.now().isoformat(),
                source="metadataApp"
            ),
            data=result,
            eventType="EventMediaMetadataSearchPerformed"
        )
        return producedEvent

    async def __getMetadata(self, titles: List[str]) -> Metadata | None:
        mal = Mal(titles=titles)
        anii = Anii(titles=titles)
        imdb = Imdb(titles=titles)

        results: List[Metadata | None] = await asyncio.gather(
            mal.search(),
            anii.search(),
            imdb.search()
        )

        filtered_results = [result for result in results if result is not None]
        logger.info("\nSimple matcher")
        simpleSelector = SimpleMatcher(titles=titles, metadata=filtered_results).getBestMatch()
        logger.info("\nAdvanced matcher")
        advancedSelector = AdvancedMatcher(titles=titles, metadata=filtered_results).getBestMatch()
        logger.info("\nPrefix matcher")
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

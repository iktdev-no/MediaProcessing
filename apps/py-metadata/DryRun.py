import logging
import signal
import sys
import os
from typing import List, Optional
import uuid
import threading
import json
import time, asyncio
from fuzzywuzzy import fuzz

from algo.AdvancedMatcher import AdvancedMatcher
from algo.SimpleMatcher import SimpleMatcher
from algo.PrefixMatcher import PrefixMatcher
from models.metadata import Metadata

from clazz.shared import EventData, EventMetadata, MediaEvent
from app import MetadataEventHandler
from sources.anii import Anii
from sources.imdb import Imdb
from sources.mal import Mal


log_level = os.environ.get("LOG_LEVEL") or None

configured_level = logging.INFO
if (log_level is not None):
    _log_level = log_level.lower()    
    if (_log_level.startswith("d")):
        configured_level = logging.DEBUG
    elif (_log_level.startswith("e")):
        configured_level = logging.ERROR
    elif (_log_level.startswith("w")):
        configured_level = logging.WARNING
    



# Konfigurer logging
logging.basicConfig(
    level=configured_level,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

if (configured_level == logging.DEBUG):
    logger.info("Logger configured with DEBUG")
elif (configured_level == logging.ERROR):
    logger.info("Logger configured with ERROR")
elif (configured_level == logging.WARNING):
    logger.info("Logger configured with WARNING")
else:
    logger.info("Logger configured with INFO")


class DryRun():
    searchTitles: List[str] = []
    title: str
    sanitizedName: str

    def __init__(self, title: str, sanitizedName: str, searchTitles: List[str]) -> None:
        self.title = title
        self.sanitizedName = sanitizedName
        self.searchTitles = searchTitles
    
    def run(self) -> None:
        evnet = MediaEvent(
            metadata=EventMetadata(
                derivedFromEventId="ccccccccc-cccc-cccc-cccc-cccccccccccc",
                eventId="eeeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
                referenceId="rrrrrrrrr-rrrr-rrrr-rrrr-rrrrrrrrrrrr",
                status="Success",
                created="2024-12-28T16:19:31.917684523",
                source="DryRun"
            ),
            eventType="DryRun",
            data=EventData(
                title=self.title,
                sanitizedName=self.sanitizedName,
                searchTitles=self.searchTitles
            )
        )
        
        handler = MetadataEventHandler(evnet)
        
        asyncio.run(handler.run())
        
    
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
        if prefixSelector is not None:
            return prefixSelector
        if simpleSelector is not None:
            return simpleSelector
        if advancedSelector is not None:
            return advancedSelector
        return None
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

from algo.AdvancedMatcher import AdvancedMatcher
from algo.SimpleMatcher import SimpleMatcher
from algo.PrefixMatcher import PrefixMatcher
from clazz.KafkaMessageSchema import KafkaMessage, MessageDataWrapper
from clazz.Metadata import Metadata

from sources.anii import Anii
from sources.imdb import Imdb
from sources.mal import Mal


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


class DryRun():
    titles: List[str] = []

    def __init__(self, titles: List[str]) -> None:
        self.titles = titles
    
    def run(self) -> None:
        combined_titles = ", ".join(self.titles)
        logger.info("Searching for %s", combined_titles)
        result: Metadata | None = self.__getMetadata(self.titles)

        message: str | None = None
        if (result is None):
            message = f"No result for {combined_titles}"
            logger.info(message)

        messageData = MessageDataWrapper(
            status =  "ERROR" if result is None else "COMPLETED",
            message =  message,
            data = result,
            derivedFromEventId = None
        )

        producerMessage = KafkaMessage(referenceId="DryRun..", data=messageData).to_json()
        logger.info(producerMessage)
    
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
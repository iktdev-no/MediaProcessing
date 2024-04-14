import logging
from dataclasses import dataclass
from typing import List, Optional
from .result import Metadata, DataResult
from .anii import metadata as AniiMetadata
from .imdb import metadata as ImdbMetadata
from .mal import metadata as MalMetadata
from fuzzywuzzy import fuzz
from unidecode import unidecode
import json
import re

logger = logging.getLogger(__name__)

@dataclass
class WeightedData:
    result: DataResult
    weight: int = 1

@dataclass
class DataAndScore:
    result: DataResult = None
    score: int = 0

class UseSource():
    title: str
    evnetId: str
    def __init__(self, title, eventId) -> None:
        self.title = title

    def stripped(self, input_string) -> str:
        return re.sub(r'[^a-zA-Z0-9]', '', input_string)

    def __perform_search(self, title)-> List[WeightedData]:
        anii = AniiMetadata(title).lookup()
        imdb = ImdbMetadata(title).lookup()
        mal = MalMetadata(title).lookup()

        result: List[WeightedData] = []
        if (anii is not None) and (anii.status == "COMPLETED" and anii.data is not None):
            result.append(WeightedData(anii, 2))
        if (imdb is not None) and (imdb.status == "COMPLETED" and imdb.data is not None):
            imdb_weight = 1
            #if (imdb.data.title == title or unidecode(self.stripped(imdb.data.title)) == unidecode(self.stripped(title))): To use after test
            if (imdb.data.title == title or unidecode(imdb.data.title) == unidecode(title)):
                imdb_weight = 10
            result.append(WeightedData(imdb, imdb_weight))
        if (mal is not None) and (mal.status == "COMPLETED" and mal.data is not None):
            result.append(WeightedData(mal, 4))
        return result
    
    def __calculate_score(self, title: str, weightData: List[WeightedData]) -> List[DataAndScore]:
        """"""
        result: List[WeightedData] = []
        for wd in weightData:
            highScore = fuzz.ratio(self.stripped(title.lower()), self.stripped(wd.result.data.title.lower()))
            for name in wd.result.data.altTitle:
                altScore = fuzz.ratio(self.stripped(title.lower()), self.stripped(name.lower()))
                if (altScore > highScore):
                    highScore = altScore
            givenScore = highScore * (wd.weight / 10)
            result.append(DataAndScore(wd.result, givenScore))
        return result


    def select_result(self) -> Optional[DataResult]:
        """""" 
        weightResult = self.__perform_search(title=self.title)
        scored = self.__calculate_score(title=self.title, weightData=weightResult)
        scored.sort(key=lambda x: x.score, reverse=True)

        try:
            jsr = json.dumps(scored, indent=4)
            with open(f"./logs/{self.evnetId}.json", "w", encoding="utf-8") as f:
                f.write(jsr)
        except:
            logger.info("Couldn't dump log..")
            logger.info(jsr)

        try:
            titles: List[str] = []
            for wd in weightResult:
                titles.append(wd.result.data.title)
                titles.extend(wd.result.data.altTitle)
            joinedTitles = "\n\t" + "\n\t".join(titles)
            logger.info(f"[Title]: {self.title} gave the result: {joinedTitles} \nTitle selected: \n\t{scored[0].result.data.title}\n")
        except:
            pass

        # Return the result with the highest score (most likely result)
        return scored[0].result if scored else None


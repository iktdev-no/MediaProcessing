import logging
from dataclasses import dataclass, asdict
from typing import List, Optional
from .result import Metadata, DataResult, NamedDataResult
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

    def to_dict(self):
        return asdict(self)    

@dataclass
class DataAndScore:
    result: DataResult = None
    score: int = 0
    def to_dict(self):

        return asdict(self)    

class UseSource():
    title: str
    baseName: str
    eventId: str
    def __init__(self, title, baseName, eventId) -> None:
        self.title = title
        self.baseName = baseName
        self.eventId = eventId

    def stripped(self, input_string) -> str:
        unitext = unidecode(input_string)
        unitext = re.sub(r'[^a-zA-Z0-9\s]', '', unitext)
        unitext = re.sub(r'\s{2,}', '', unitext)
        return unitext

    def __perform_search(self, title)-> List[WeightedData]:
        anii = AniiMetadata(title).lookup()
        imdb = ImdbMetadata(title).lookup()
        mal = MalMetadata(title).lookup()

        result: List[WeightedData] = []
        if (anii is not None) and (anii.status == "COMPLETED" and anii.data is not None):
            result.append(WeightedData(anii, 1.2))
        if (imdb is not None) and (imdb.status == "COMPLETED" and imdb.data is not None):
            imdb_weight = 1
            if (imdb.data.title == title or self.stripped(imdb.data.title) == self.stripped(title)):
                imdb_weight = 100
            result.append(WeightedData(imdb, imdb_weight))
        if (mal is not None) and (mal.status == "COMPLETED" and mal.data is not None):
            result.append(WeightedData(mal, 1.8))
        return result
    
    def __calculate_score(self, title: str, weightData: List[WeightedData]) -> List[DataAndScore]:
        """"""
        result: List[WeightedData] = []
        for wd in weightData:
            highScore = fuzz.ratio(self.stripped(title.lower()), self.stripped(wd.result.data.title.lower()))
            logger.info(f"[H:{highScore}]\t{self.stripped(wd.result.data.title.lower())} => {self.stripped(title.lower())}")
            for name in wd.result.data.altTitle:
                altScore = fuzz.ratio(self.stripped(title.lower()), self.stripped(name.lower()))
                if (altScore > highScore):
                    highScore = altScore
                logger.info(f"[A:{highScore}]\t{self.stripped(wd.result.data.title.lower())} => {self.stripped(title.lower())}")
            givenScore = highScore * (wd.weight / 10)
            logger.info(f"[G:{givenScore}]\t{self.stripped(wd.result.data.title.lower())} => {self.stripped(title.lower())} Weight:{wd.weight}")
            result.append(DataAndScore(wd.result, givenScore))
        return result


    def select_result(self) -> Optional[NamedDataResult]:
        """""" 
        scored: List[DataAndScore] = []
        titleResult = self.__perform_search(title=self.title)
        baseNameResult = self.__perform_search(title=self.baseName)

        titleScoreResult = self.__calculate_score(title=self.title, weightData=titleResult)
        baseNameScoreResult = self.__calculate_score(title=self.baseName, weightData=baseNameResult)
        
        titleScoreResult.sort(key=lambda x: x.score, reverse=True)
        baseNameScoreResult.sort(key=lambda x: x.score, reverse=True)

        scored.extend(titleScoreResult)
        scored.extend(baseNameScoreResult)

        selected: NamedDataResult|None = None
        ht = titleScoreResult[0]
        bt = baseNameScoreResult[0]
        if (bt is not None and ht is not None):
            if (bt.score >= ht.score):
                selected = NamedDataResult(self.baseName, bt)
            else:
                selected = NamedDataResult(self.title, ht)
        else:
            if len(titleScoreResult) > 0:
                selected = NamedDataResult(self.title, titleScoreResult[0])
            elif len(baseNameScoreResult) > 0:
                selected = NamedDataResult(self.baseName, baseNameScoreResult[0])
            else:
                selected = None
        
        jsr = ""
        try:
            jsr = json.dumps([obj.to_dict() for obj in scored], indent=4)
            with open(f"./logs/{self.eventId}.json", "w", encoding="utf-8") as f:
                f.write(jsr)
        except Exception as e:
            logger.info("Couldn't dump log..")
            logger.error(e)
            logger.info(jsr)

        try:
            titledResult = titleResult
            titledResult.extend(baseNameResult)

            titles: List[str] = []
            for wd in titledResult:
                titles.append(wd.result.data.title)
                titles.extend(wd.result.data.altTitle)
            joinedTitles = "\n\t" + "\n\t".join(titles)
            logger.info(f"\nTitle: {self.title} \nBaseName: {self.baseName} \nFound: {joinedTitles} \nTitle selected: \n\t{selected.result.data.title}\n")
        except Exception as e:
            logger.error(e)
            pass

        # Return the result with the highest score (most likely result)
        return selected


import logging
from dataclasses import dataclass
from typing import List, Optional
from .result import Metadata, DataResult
from .anii import metadata as AniiMetadata
from .imdb import metadata as ImdbMetadata
from .mal import metadata as MalMetadata
from fuzzywuzzy import fuzz

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
    def __init__(self, title) -> None:
        self.title = title

    def __perform_search(self, title)-> List[WeightedData]:
        anii = AniiMetadata(title).lookup()
        imdb = ImdbMetadata(title).lookup()
        mal = MalMetadata(title).lookup()

        result: List[WeightedData] = []
        if (anii is not None) and (anii.status == "COMPLETED" and anii.data is not None):
            result.append(WeightedData(anii, 4))
        if (imdb is not None) and (imdb.status == "COMPLETED" and imdb.data is not None):
            imdb_weight = 1
            if (imdb.data.title == title):
                imdb_weight = 7
            result.append(WeightedData(imdb, imdb_weight))
        if (mal is not None) and (mal.status == "COMPLETED" and mal.data is not None):
            result.append(WeightedData(mal, 8))
        return result
    
    def __calculate_score(self, title: str, weightData: List[WeightedData]) -> List[DataAndScore]:
        """"""
        result: List[WeightedData] = []
        for wd in weightData:
            highScore = fuzz.ratio(title.lower(), wd.result.data.title.lower())
            for name in wd.result.data.altTitle:
                altScore = fuzz.ratio(title.lower(), name.lower())
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
            titles: List[str] = []
            for wd in weightResult:
                titles.append(wd.result.data.title)
                titles.extend(wd.result.data.altTitle)
            joinedTitles = "\n\t".join(titles)
            logger.info(f"Title {self.title} gave the result: {joinedTitles} \nWith used: {scored[0].result.data.title}\n")
        except:
            pass

        # Return the result with the highest score (most likely result)
        return scored[0].result if scored else None


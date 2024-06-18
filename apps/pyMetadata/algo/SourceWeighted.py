from dataclasses import dataclass
from typing import List, Optional
from fuzzywuzzy import fuzz
from unidecode import unidecode
import logging
import re

from clazz.Metadata import Metadata

log = logging.getLogger(__name__)

@dataclass
class WeightedData:
    result: Metadata
    weight: float

@dataclass
class DataAndScore:
    result: Metadata
    score: float
    weight: float
    matched_title: str


class UseSource:
    titles: List[str] = []
    dataWeighed: List[WeightedData] = []

    def __init__(self, titles: List[str], mal: Optional[Metadata] = None, imdb: Optional[Metadata] = None, anii: Optional[Metadata] = None) -> None:
        self.titles = titles
        if mal is not None:
            self.dataWeighed.append(WeightedData(mal, 1.5))

        if imdb is not None:
            self.dataWeighed.append(WeightedData(imdb, 1))
        
        if anii is not None:
            self.dataWeighed.append(WeightedData(anii, 1.3))


    def stripped(self, input_string) -> str:
        unitext = unidecode(input_string)
        unitext = re.sub(r'[^a-zA-Z0-9\s]', ' ', unitext)
        unitext = re.sub(r'\s{2,}', ' ', unitext)
        return unitext.strip()

    
    def __calculate_score(self, title: str, weightData: List[WeightedData]) -> List[DataAndScore]:
        result: List[DataAndScore] = []

        for title_to_check in self.titles:
            for wd in weightData:
                if wd.result is None:
                    continue
                
                highScore = fuzz.ratio(self.stripped(title_to_check.lower()), self.stripped(wd.result.title.lower()))
                for alt_title in wd.result.altTitle:
                    altScore = fuzz.ratio(self.stripped(title_to_check.lower()), self.stripped(alt_title.lower()))
                    if altScore > highScore:
                        highScore = altScore
                givenScore = highScore * wd.weight
                result.append(DataAndScore(wd.result, givenScore, wd.weight, title_to_check))
        
        result.sort(key=lambda x: x.score, reverse=True)
        return result

    def select_result_table(self) -> Optional[pd.DataFrame]:
        scoredResults = []
        for title in self.titles:
            scoredResult = self.__calculate_score(title=title, weightData=self.dataWeighed)
            scoredResults.append(scoredResult)

        all_results = [item for sublist in scoredResults for item in sublist]

        if not all_results:
            return None

        # Prepare data for DataFrame
        data = {
            "Title": [],
            "Alt Title": [],
            "Score": [],
            "Weight": [],
            "Matched Title": []
        }

        for ds in all_results:
            metadata = ds.result
            data["Title"].append(metadata.title)
            data["Alt Title"].append(", ".join(metadata.altTitle))
            data["Score"].append(ds.score)
            data["Weight"].append(ds.weight)
            data["Matched Title"].append(ds.matched_title)

        df = pd.DataFrame(data)
        df = df.sort_values(by="Score", ascending=False).reset_index(drop=True)

        try:
            df.to_json(f"./logs/{self.titles[0]}.json", orient="records", indent=4)
        except Exception as e:
            log.error(f"Failed to dump JSON: {e}")

        return df

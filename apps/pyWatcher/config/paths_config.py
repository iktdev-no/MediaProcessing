import os
from dataclasses import dataclass
from typing import List

@dataclass
class PathsConfig:
    watch_paths: List[str]
    extensions: List[str]

    @staticmethod
    def from_env() -> "PathsConfig":
        # Paths kan legges inn som kommaseparert liste i miljøvariabel
        raw_paths = os.environ.get("WATCH_PATHS")
        paths = [p.strip() for p in raw_paths.split(",") if p.strip()]

        # Extensions kan legges inn som kommaseparert liste
        raw_ext = os.environ.get("WATCH_EXTENSIONS") or ".mkv,.mp4,.avi"
        extensions = [e.strip() for e in raw_ext.split(",") if e.strip()]

        return PathsConfig(watch_paths=paths, extensions=extensions)

    def validate(self) -> None:
        if not self.watch_paths:
            raise ValueError("Ingen paths definert for overvåkning")
        for path in self.watch_paths:
            if not os.path.exists(path):
                raise ValueError(f"Path finnes ikke: {path}")
            if not os.path.isdir(path):
                raise ValueError(f"Path er ikke en katalog: {path}")
        if not self.extensions:
            raise ValueError("Ingen filendelser definert for filtrering")

# config/source_activation_loader.py

import json
from pathlib import Path
import os


DEFAULT_CONFIG = {
    "anii": True,
    "mal": True,
    "imdb": True,
    "tmdb": True,
}

def running_in_docker() -> bool:
    try:
        with open("/proc/1/cgroup", "rt") as f:
            content = f.read()
            return "docker" in content or "kubepods" in content
    except Exception:
        return False


def get_config_path() -> Path:
    if running_in_docker():
        return Path("/data/sources.json")
    else:
        return Path(__file__).parent / "sources.json"


def load_source_config() -> dict[str, bool]:
    path = get_config_path()

    # Ensure directory exists
    if not path.parent.exists():
        path.parent.mkdir(parents=True, exist_ok=True)

    # If file doesn't exist → create with defaults
    if not path.exists():
        with open(path, "w", encoding="utf-8") as f:
            json.dump(DEFAULT_CONFIG, f, indent=4)
        config = DEFAULT_CONFIG.copy()
    else:
        try:
            with open(path, "r", encoding="utf-8") as f:
                config = json.load(f)

            # Ensure all keys exist
            for key in DEFAULT_CONFIG:
                if key not in config:
                    config[key] = DEFAULT_CONFIG[key]

        except Exception:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(DEFAULT_CONFIG, f, indent=4)
            config = DEFAULT_CONFIG.copy()

    # ---------------------------------------------------------
    # TMDB auto-disable if API key is missing
    # ---------------------------------------------------------
    if not os.getenv("TMDB_API_KEY"):
        if config.get("tmdb", True):
            print("[source-loader] TMDB disabled: missing TMDB_API_KEY")
        config["tmdb"] = False


    return config

# config/source_priority_loader.py

import json
import os
from pathlib import Path

DEFAULT_PRIORITY = {
    "mal": 1,
    "aniiv2": 2,
    "imdb": 3,
    "anii": 4
}

def running_in_docker() -> bool:
    try:
        with open("/proc/1/cgroup", "rt") as f:
            content = f.read()
            return "docker" in content or "kubepods" in content
    except Exception:
        return False


def get_priority_config_path() -> Path:
    if running_in_docker():
        return Path("/data/source_priority.json")
    else:
        return Path(__file__).parent / "source_priority.json"


def load_source_priority() -> dict:
    path = get_priority_config_path()

    # Ensure directory exists
    if not path.parent.exists():
        path.parent.mkdir(parents=True, exist_ok=True)

    # If file doesn't exist → create with defaults
    if not path.exists():
        with open(path, "w", encoding="utf-8") as f:
            json.dump(DEFAULT_PRIORITY, f, indent=4)
        return DEFAULT_PRIORITY.copy()

    # Try to load existing config
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        # Validate keys (optional)
        for key in DEFAULT_PRIORITY:
            if key not in data:
                data[key] = DEFAULT_PRIORITY[key]

        return data

    except Exception:
        # If file is corrupted → rewrite with defaults
        with open(path, "w", encoding="utf-8") as f:
            json.dump(DEFAULT_PRIORITY, f, indent=4)
        return DEFAULT_PRIORITY.copy()

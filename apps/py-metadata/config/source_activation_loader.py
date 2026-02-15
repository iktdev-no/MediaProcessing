# config/source_activation_loader.py

import json
from pathlib import Path

DEFAULT_CONFIG = {
    "aniiv2": True,
    "mal": True,
    "imdb": True,
    "anii": False
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


def load_source_config() -> dict:
    path = get_config_path()

    # Ensure directory exists
    if not path.parent.exists():
        path.parent.mkdir(parents=True, exist_ok=True)

    # If file doesn't exist → create with defaults
    if not path.exists():
        with open(path, "w", encoding="utf-8") as f:
            json.dump(DEFAULT_CONFIG, f, indent=4)
        return DEFAULT_CONFIG.copy()

    # Try to load existing config
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        # Validate keys (optional)
        for key in DEFAULT_CONFIG:
            if key not in data:
                data[key] = DEFAULT_CONFIG[key]

        return data

    except Exception:
        # If file is corrupted → rewrite with defaults
        with open(path, "w", encoding="utf-8") as f:
            json.dump(DEFAULT_CONFIG, f, indent=4)
        return DEFAULT_CONFIG.copy()

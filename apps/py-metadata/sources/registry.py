from typing import Dict
from config.source_activation_loader import load_source_config

# Global cached config
SOURCE_CONFIG: Dict[str, bool] = {}

def init_source_config():
    global SOURCE_CONFIG
    SOURCE_CONFIG.clear()
    SOURCE_CONFIG.update(load_source_config())
    print(f"[SOURCE CONFIG LOADED ONCE] {SOURCE_CONFIG}")

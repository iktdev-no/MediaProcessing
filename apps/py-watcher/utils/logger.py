import logging
import sys

# ANSI farger
COLORS = {
    "INFO": "\033[94m",    # blå
    "DEBUG": "\033[92m",   # grønn
    "WARNING": "\033[93m", # gul
    "ERROR": "\033[91m",   # rød
    "RESET": "\033[0m"
}

class ColoredFormatter(logging.Formatter):
    def format(self, record):
        levelname = record.levelname
        color = COLORS.get(levelname, COLORS["RESET"])
        prefix = f"[{levelname}]"
        message = super().format(record)
        return f"{color}{prefix}{COLORS['RESET']} {message}"

def setup_logger(level=logging.INFO):
    handler = logging.StreamHandler(sys.stdout)
    formatter = ColoredFormatter("%(asctime)s - %(name)s - %(message)s")
    handler.setFormatter(formatter)

    logger = logging.getLogger()
    logger.setLevel(level)
    logger.handlers = [handler]
    return logger

# Opprett global logger
logger: logging.Logger = setup_logger()

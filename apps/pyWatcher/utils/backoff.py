from utils.logger import logger
import time

def retry_delays():
    return [5, 15, 30, 60]

def wait_with_backoff():
    for delay in retry_delays():
        logger.info(f"⏳ Venter {delay} sekunder...")
        time.sleep(delay)
        yield

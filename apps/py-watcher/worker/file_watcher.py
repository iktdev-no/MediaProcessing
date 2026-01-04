from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
import asyncio
from utils.file_handler import FileHandler
from utils.readiness import check_ready
from utils.logger import logger

class Handler(FileSystemEventHandler):
    def __init__(self, db, extensions, insert_event):
        self.db = db
        self.file_handler = FileHandler(extensions)
        self.insert_event = insert_event

    def on_created(self, event):
        if event.is_directory:
            return
        ev = self.file_handler.handle_created(event.src_path)
        if ev:
            self.insert_event(self.db, ev)
            asyncio.create_task(check_ready(self.db, ev.referenceId, ev.data.fileName, ev.data.fileUri, self.insert_event))
            logger.info(f"➕ FileAddedEvent persisted for {ev.data.fileName}")

    def on_deleted(self, event):
        if event.is_directory:
            return
        ev = self.file_handler.handle_deleted(event.src_path)
        if ev:
            self.insert_event(self.db, ev)
            logger.info(f"➖ FileRemovedEvent persisted for {ev.data.fileName}")

def start_observer(db, path, extensions, insert_event):
    observer = Observer()
    handler = Handler(db, extensions, insert_event)
    observer.schedule(handler, path, recursive=True)
    observer.start()
    logger.info(f"👀 Watching path: {path}")
    return observer


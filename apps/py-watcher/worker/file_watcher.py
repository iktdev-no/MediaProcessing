from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
import asyncio
from concurrent.futures import ThreadPoolExecutor
from functools import partial
from models.event import Event, FileAddedEvent
from utils.file_handler import FileHandler
from utils.readiness import check_ready
from utils.logger import logger

class Handler(FileSystemEventHandler):
    def __init__(self, db, extensions, insert_event, loop):
        self.db = db
        self.file_handler = FileHandler(extensions)
        self.insert_event = insert_event
        self.loop = loop

    def _schedule_ready_check(self, ev: FileAddedEvent):
        asyncio.run_coroutine_threadsafe(
            check_ready(
                self.db,
                ev.referenceId,
                ev.data.fileName,
                ev.data.fileUri,
                self.insert_event
            ),
            self.loop
        )

    def on_created(self, event):
        if event.is_directory:
            return
        ev = self.file_handler.handle_created(event.src_path)
        if ev:
            self.insert_event(self.db, ev)
            logger.info(f"➕ Added: {ev.data.fileName}")
            self._schedule_ready_check(ev)

    def on_modified(self, event):
        if event.is_directory:
            return
        ev = self.file_handler.handle_modified(event.src_path)
        if ev:
            self.insert_event(self.db, ev)
            logger.info(f"✏️ Changed: {ev.data.fileName}")
            # Only schedule readiness for Added
            if isinstance(ev, FileAddedEvent):
                self._schedule_ready_check(ev)

    def on_deleted(self, event):
        if event.is_directory:
            return
        ev = self.file_handler.handle_deleted(event.src_path)
        if ev:
            self.insert_event(self.db, ev)
            logger.info(f"➖ Removed: {ev.data.fileName}")


def start_observer(db, path, extensions, insert_event, loop):
    observer = Observer()
    handler = Handler(db, extensions, insert_event, loop)
    observer.schedule(handler, path, recursive=True)
    observer.start()
    logger.info(f"👀 Watching: {path}")
    return observer

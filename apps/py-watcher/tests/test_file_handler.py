import os
import pytest
from utils.file_handler import FileHandler
from models.event import FileAddedEvent, FileRemovedEvent

def test_handle_created_returns_event_for_valid_extension(tmp_path):
    handler = FileHandler(extensions={".csv"})
    file_path = tmp_path / "test.csv"
    file_path.write_text("dummy")

    ev = handler.handle_created(str(file_path))
    assert isinstance(ev, FileAddedEvent)
    assert ev.data.fileName == "test.csv"
    assert ev.data.fileUri == str(file_path)

def test_handle_deleted_returns_event_for_valid_extension(tmp_path):
    handler = FileHandler(extensions={".csv"})
    file_path = tmp_path / "test.csv"
    file_path.write_text("dummy")

    # Filen må være "kjent" først
    handler.handle_created(str(file_path))

    ev = handler.handle_deleted(str(file_path))

    assert isinstance(ev, FileRemovedEvent)
    assert ev.data.fileName == "test.csv"

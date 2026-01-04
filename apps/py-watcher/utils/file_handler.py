import os
from models.event import Event, create_event, FileAddedEvent, FileRemovedEvent

class FileHandler:
    def __init__(self, extensions):
        self.extensions = extensions
        self.file_refs = {}

    def get_ref_id(self, path: str) -> str:
        if path in self.file_refs:
            return self.file_refs[path]
        ref_id = os.path.basename(path) + "-" + os.urandom(4).hex()
        self.file_refs[path] = ref_id
        return ref_id

    def handle_created(self, path: str) -> FileAddedEvent:
        if os.path.splitext(path)[1] not in self.extensions:
            return None
        ref_id = self.get_ref_id(path)
        return create_event(FileAddedEvent, os.path.basename(path), path, reference_id=ref_id)

    def handle_deleted(self, path: str) -> FileRemovedEvent:
        if os.path.splitext(path)[1] not in self.extensions:
            return None
        ref_id = self.get_ref_id(path)
        return create_event(FileRemovedEvent, os.path.basename(path), path, reference_id=ref_id)

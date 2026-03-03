import os
import uuid
from models.event import Event, FileChangedEvent, create_event, FileAddedEvent, FileRemovedEvent

class FileHandler:
    def __init__(self, extensions):
        self.extensions = set(extensions)
        self.file_refs = {}  # path -> referenceId

    def _is_supported(self, path):
        return os.path.splitext(path)[1].lower() in {e.lower() for e in self.extensions}

    def _get_or_create_ref_id(self, path):
        if path in self.file_refs:
            return self.file_refs[path]
        ref_id = str(uuid.uuid4())
        self.file_refs[path] = ref_id
        return ref_id

    def _get_ref_id_if_known(self, path):
        return self.file_refs.get(path)

    def handle_created(self, path):
        if not self._is_supported(path):
            return None
        ref_id = self._get_or_create_ref_id(path)
        return create_event(FileAddedEvent, os.path.basename(path), path, reference_id=ref_id)

    def handle_modified(self, path):
        if not self._is_supported(path):
            return None
        ref_id = self._get_ref_id_if_known(path)
        if ref_id:
            return create_event(FileChangedEvent, os.path.basename(path), path, reference_id=ref_id)
        ref_id = self._get_or_create_ref_id(path)
        return create_event(FileAddedEvent, os.path.basename(path), path, reference_id=ref_id)

    def handle_deleted(self, path):
        if not self._is_supported(path):
            return None
        ref_id = self._get_ref_id_if_known(path)
        if not ref_id:
            return None
        del self.file_refs[path]
        return create_event(FileRemovedEvent, os.path.basename(path), path, reference_id=ref_id)

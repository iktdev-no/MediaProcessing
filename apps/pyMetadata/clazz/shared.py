
from typing import Any, List
import json


suppress_ignore: List[str] = [
    "event:media-process:started",
    "event:request-process:started",
    "event::save",
    "event:media-process:completed",
    "event:work-encode:created",
    "event:work-extract:created",
    "event:work-convert:created",
    "event:work-encode:performed",
    "event:work-extract:performed",
    "event:work-convert:performed",    
    "event:media-read-out-cover:performed",
    "event:work-download-cover:performed",
    "event:media-read-out-name-and-type:performed",
    "event:media-parse-stream:performed",
    "event:media-extract-parameter:created",
    "event:media-encode-parameter:created",
    "event:media-metadata-search:performed"
]

consume_on_key: List[str] = [
    "request:metadata:obtain",
    "event:media-read-base-info:performed"
]

def decode_key(key_bytes: bytes | None):
    return key_bytes.decode('utf-8') if key_bytes else None

def decode_value(value_bytes: bytes | None):
    return json.loads(value_bytes.decode('utf-8')) if value_bytes else None



class ConsumerRecord:
    topic: str
    partition: int
    offset: int
    key: str
    value: str | None
    timestamp: int

    def __init__(self, message: Any) -> None:
        if message is not None:
            self.key = message.key
            self.value = message.value
            self.topic = message.topic
            self.offset = message.offset
            self.partition = message.partition
            self.timestamp = message.timestamp


class MediaEvent():
    __consumerRecord: ConsumerRecord
    referenceId: str
    eventId: str
    data: dict | None

    def __init__(self, message: ConsumerRecord) -> None:
        self.__consumerRecord = message
        self.referenceId = message.value["referenceId"]
        self.eventId = message.value["eventId"]
        self.data = message.value["data"] if "data" in message.value else None

    def isConsumable(self) -> bool:
        if "status" in self.data:
            if self.data["status"] == "COMPLETED":
                return True
        return False


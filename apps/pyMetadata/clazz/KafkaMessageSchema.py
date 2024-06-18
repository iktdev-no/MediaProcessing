

from dataclasses import asdict, dataclass
import uuid, json

from .Metadata import Metadata


@dataclass
class MessageDataWrapper:
    status: str # COMPLETED / ERROR
    message: str | None
    data: Metadata | None
    derivedFromEventId: str | None

    def to_dict(self):
        return asdict(self)


class KafkaMessage:
    referenceId: str
    eventId: str = str(uuid.uuid4())
    data: MessageDataWrapper

    def __init__(self, referenceId: str, data: MessageDataWrapper) -> None:
        self.referenceId = referenceId
        self.data = data
        pass

    def to_json(self):
        payload = {
            'referenceId': self.referenceId,
            'eventId': self.eventId,
            'data': self.data.to_dict() if self.data else None
        }
        return json.dumps(payload)

    
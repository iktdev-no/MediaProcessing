import json
from dataclasses import dataclass
from typing import Any, List, Optional
from datetime import datetime

# Definer dataclassene for strukturen
@dataclass
class EventMetadata:
    derivedFromEventId: str
    eventId: str
    referenceId: str
    status: str
    created: datetime

@dataclass
class EventData:
    title: str
    sanitizedName: str
    searchTitles: List[str]

@dataclass
class MediaEvent:
    metadata: EventMetadata
    eventType: str
    data: Any| EventData

# Funksjon for å parse datetime fra streng
def parse_datetime(datetime_str: str) -> datetime:
    return datetime.fromisoformat(datetime_str)

def event_data_to_json(event_data: EventData) -> str:
    return json.dumps(event_data.__dict__)

# Funksjon for å konvertere JSON til klasser
def json_to_media_event(json_data: str) -> MediaEvent:
    data_dict = json.loads(json_data)

    metadata_dict = data_dict['metadata']
    event_data_dict = data_dict['data']

    metadata = EventMetadata(
        derivedFromEventId=metadata_dict['derivedFromEventId'],
        eventId=metadata_dict['eventId'],
        referenceId=metadata_dict['referenceId'],
        status=metadata_dict['status'],
        created=parse_datetime(metadata_dict['created'])
    )

    event_data = EventData(
        title=event_data_dict['title'],
        sanitizedName=event_data_dict['sanitizedName'],
        searchTitles=event_data_dict['searchTitles']
    )

    media_event = MediaEvent(
        metadata=metadata,
        eventType=data_dict['eventType'],
        data=event_data
    )

    return media_event
import { type } from "os";


interface ExplorerCursor {
    name: string
    path: string
    items: Array<ExplorerItem>
}


type ExplorerItemType = "FILE" | "FOLDER";

interface ExplorerItem {
    path: string;
    name: string;
    extension: string|null;
    created: number,
    type: ExplorerItemType
}


interface EventDataObject {
    id: string;
    details: Details
    encode: Encode
    io: IO
    events: Array<string>
}

interface Details {
    title: string;
    file: string;
    sanitizedName: string
    collection: string|null
}

interface Metadata {
    source: string
}

interface Encode {
    state: string;
    progress: number = 0
}

interface IO {
    inputFile: string;
    outputFile: string;
}


enum SimpleEventDataState {
    NA,
    QUEUED,
    STARTED,
    ENDED,
    FAILED,
  }
  
interface SimpleEventDataObject {
    id: string;
    name?: string | null;
    path?: string | null;
    givenTitle?: string | null;
    givenSanitizedName?: string | null;
    givenCollection?: string | null;
    determinedType?: string | null;
    eventEncoded: SimpleEventDataState;
    eventExtracted: SimpleEventDataState;
    eventConverted: SimpleEventDataState;
    eventCollected: SimpleEventDataState;
    encodingProgress?: number | null;
    encodingTimeLeft?: number | null;
}

interface EventObject {
    referenceId: string;
    eventId: string;
    event: string;
    data: string;
    created: string;
}

interface EventsObjectList {
    referenceId: string;
    events: Array<EventObject>;
}

interface EventsObjectListResponse {
    lastPull: string;
    items: Array<EventsObjectList>;
}
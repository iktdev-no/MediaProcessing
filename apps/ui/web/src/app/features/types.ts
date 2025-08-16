export interface CoordinatorOperationRequest {
    destination: string;
    file: string;
    source: string;
    mode: "FLOW" | "MANUAL";
}

export interface Event {
   referenceId: string;
   eventId: string;
   event: string;
   data: any;
   created: number;
}

export interface DatabaseEventEntry {
    referenceId: string;
    events: Array<Event>
    created: number;
    lastEventCreated: number;
} 
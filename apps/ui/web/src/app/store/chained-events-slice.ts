import { createSlice, PayloadAction } from "@reduxjs/toolkit"
import { ExpandableTableItem } from "../features/table/expandableTable";

export interface EventGroup extends ExpandableTableItem {
    referenceId: string,
    created: number,
    fileName: string|null,
    events: EventChain[]
}

export interface EventChain {
    eventId: string,
    eventName: string,
    created: number,
    success: boolean,
    skipped: boolean,
    failure: boolean,
    events: Array<EventChain>
}

export interface EventGroups {
    groups: Array<EventGroup>
}

const initialState: EventGroups = {
    groups: []
};


const chainedEventsSlice = createSlice({
    name: "ChainedEvents",
    initialState,
    reducers: {
      set: (state, action: PayloadAction<Array<EventGroup>>) => {
        state.groups = action.payload;
      },
    },
})

export const { set } = chainedEventsSlice.actions;
export default chainedEventsSlice.reducer;
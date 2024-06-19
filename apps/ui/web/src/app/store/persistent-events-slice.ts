import { PayloadAction, createSlice } from "@reduxjs/toolkit"
import { EventsObjectList, EventsObjectListResponse } from "../../types";

interface PersistentEventsState {
    items: Array<EventsObjectList>
    lastPull: string|null
}

const initialState: PersistentEventsState = {
    items: [],
    lastPull: null
}

const persistentEventSlice = createSlice({
    name: "PersistentEvents",
    initialState,
    reducers: {
        set(state, action: PayloadAction<EventsObjectListResponse>) {
            state.items = action.payload.items;
            state.lastPull = action.payload.lastPull;
        }
    }
});

export const { set } = persistentEventSlice.actions;
export default persistentEventSlice.reducer;
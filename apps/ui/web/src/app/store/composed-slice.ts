import { PayloadAction, createSlice } from "@reduxjs/toolkit"
import { EventDataObject } from "../../types"

interface ComposedState {
    items: Array<EventDataObject>
}

const initialState: ComposedState = {
    items: []
}

const composedSlice = createSlice({
    name: "Composed",
    initialState,
    reducers: {
        updateItems(state, action: PayloadAction<Array<EventDataObject>>) {
            state.items = action.payload
        }
    }
})

export const { updateItems } = composedSlice.actions;
export default composedSlice.reducer;
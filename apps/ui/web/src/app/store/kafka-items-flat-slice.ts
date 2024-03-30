import { PayloadAction, createSlice } from "@reduxjs/toolkit"
import { SimpleEventDataObject } from "../../types"

interface ComposedState {
    items: Array<SimpleEventDataObject>
}

const initialState: ComposedState = {
    items: []
}

const kafkaComposedFlat = createSlice({
    name: "Composed",
    initialState,
    reducers: {
        simpleEventsUpdate(state, action: PayloadAction<Array<SimpleEventDataObject>>) {
            state.items = action.payload
        }
    }
})

export const { simpleEventsUpdate } = kafkaComposedFlat.actions;
export default kafkaComposedFlat.reducer;
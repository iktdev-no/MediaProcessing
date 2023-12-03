import { PayloadAction, createSlice } from "@reduxjs/toolkit"
import { ExplorerItem, ExplorerCursor } from "../../types"

interface ExplorerState {
    name: string | null
    path: string | null
    items: Array<ExplorerItem>
}

const initialState: ExplorerState = {
    name: null,
    path: null,
    items: []
}

const composedSlice = createSlice({
    name: "Explorer",
    initialState,
    reducers: {
        updateItems(state, action: PayloadAction<ExplorerCursor>) {
            state.items = action.payload.items;
            state.name = action.payload.name;
            state.path = action.payload.path
        },
    }
})

export const { updateItems } = composedSlice.actions;
export default composedSlice.reducer;
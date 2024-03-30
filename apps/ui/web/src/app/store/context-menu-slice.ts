import { PayloadAction, createSlice } from "@reduxjs/toolkit"

interface ContextMenuPosition {
    x: number,
    y: number
}

interface ContextMenuState {
    visible: boolean,
    position?: ContextMenuPosition
}

const initialState: ContextMenuState = {
    visible: false
}

const contextMenuSlice = createSlice({
    name: 'ContextMenu',
    initialState,
    reducers: {
        setContextMenuVisible(state, action: PayloadAction<boolean>) {
            state.visible = action.payload
        },
        setContextMenuPosition(state, action: PayloadAction<ContextMenuPosition>) {
            state.position = action.payload
        }
    }
});

export const { setContextMenuVisible, setContextMenuPosition } = contextMenuSlice.actions;
export default contextMenuSlice.reducer;
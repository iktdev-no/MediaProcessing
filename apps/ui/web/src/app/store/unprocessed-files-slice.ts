import { PayloadAction, createSlice } from "@reduxjs/toolkit"
import { ExplorerItem, ExplorerCursor } from "../../types"
import { TableItemGroup } from "../features/table/multiListSortedTable"
import exp from "constants"

export interface FileInfo {
    name: string
    fileName: string
    checksum: string
}

export interface FileInfoGroup extends TableItemGroup<FileInfo> {
    title: string
    items: Array<FileInfo>
}

export interface IncomingUnprocessedFiles {
    available: Array<FileInfo>
    inProcess: Array<FileInfo>
}

const initialState: IncomingUnprocessedFiles = {
    available: [],
    inProcess: []
}

const unprocessedFilesSlice = createSlice({
    name: "UnprocessedFiles",
    initialState,
    reducers: {
        update(state, action: PayloadAction<IncomingUnprocessedFiles>) {
            state.available = action.payload.available ?? []
            state.inProcess = action.payload.inProcess ?? []
        },
    }
})

export const { update } = unprocessedFilesSlice.actions;
export default unprocessedFilesSlice.reducer;
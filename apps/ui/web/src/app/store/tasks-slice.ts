import { PayloadAction, createSlice } from "@reduxjs/toolkit"
import { TableItemGroup } from "../features/table/multiListSortedTable";

export enum TaskType {
    Encode = 'Encode',
    Extract = 'Extract',
    Convert = 'Convert'
}
  
export interface TaskData {
    inputFile: string;
}

export enum SubtitleFormats {
    SRT = 'SRT',
    VTT = 'VTT',
    ASS = 'ASS',
    SUB = 'SUB'
}
  
  
export interface EncodeArgumentData extends TaskData {
    arguments: string[];
    outputFileName: string;
}
  
  export interface ExtractArgumentData extends TaskData {
    arguments: string[];
    language: string;
    storeFileName: string;
    outputFileName: string;
}
  
  export interface ConvertData extends TaskData {
    language: string;
    outputDirectory: string;
    outputFileName: string;
    storeFileName: string;
    formats: SubtitleFormats[];
    allowOverwrite: boolean;
}
  

export interface Task {
    referenceId: string;
    status?: string | null;
    claimed: boolean;
    claimedBy?: string | null;
    consumed: boolean;
    task: TaskType;
    eventId: string;
    derivedFromEventId?: string | null;
    data?: EncodeArgumentData | ExtractArgumentData | ConvertData | null;
    created: string; // Bruk ISO-dato som string
    lastCheckIn?: string | null;
  }
  

export interface TaskGroup{
    referenceId: string
    tasks: Array<Task>
}

export interface TaskGroupList {
    items: Array<TaskGroup>
}

export interface TableTaskGroup extends TableItemGroup<Task> {
    title: string
    items: Array<Task>
}   

export interface TableTaskGroupList {
    items: Array<TableTaskGroup>
}

const initialState: TableTaskGroupList = {
    items: []
}

const tasksSlice = createSlice({
    name: "Tasks",
    initialState,
    reducers: {
        update(state, action: PayloadAction<Array<TaskGroup>>) {
            state.items = action.payload.map((value) => ({
                title: value.referenceId,
                items: value.tasks
            })) ?? []
        },
    }
});

export const { update } = tasksSlice.actions;
export default tasksSlice.reducer;

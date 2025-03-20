import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useStompClient } from "react-stomp-hooks";
import { Box, Button, Grid, TextField, Typography, useTheme } from '@mui/material';
import { ExplorerItem } from "../../types";
import { ContextMenuItem } from "../features/ContextMenu";
import { RootState } from "../store";
import { useWsSubscription } from "../ws/subscriptions";
import SimpleTable from "../features/table/sortableTable";
import { TableCellCustomizer, TablePropetyConfig } from "../features/table/table";
import MultiListSortedTable from "../features/table/multiListSortedTable";
import { TableTaskGroup, Task, TaskGroup, update } from "../store/tasks-slice";
import SortableGroupedTable from "../features/table/sortableGroupedTable";
import { UnixTimestamp } from "../features/UxTc";

const columns: Array<TablePropetyConfig> = [
    { label: "Name", accessor: "data.inputFile" },
    { label: "Task", accessor: "task" },
    { label: "Status", accessor: "status" },
    { label: "Created", accessor: "created" },
];

const createTableCell: TableCellCustomizer<Task> = (accessor, data) => {
    switch (accessor) {
      case "created": {
        if (typeof data[accessor] === "string") {
          return UnixTimestamp({ timestamp: Date.parse(data[accessor]) });
        }
        return null;
      }
      case "data.inputFile": {
        const parts = data.data?.inputFile.split("/") ?? [];
        return <Typography>{parts[parts?.length - 1]}</Typography>   
    }
      default:
        return null;
    }
  };
  

export default function ProcesserTasksPage() {
    const muiTheme = useTheme();
    const dispatch = useDispatch();
    const client = useStompClient();
    const taskGroups = useSelector((state: RootState) => state.tasks);

    useWsSubscription<Array<TaskGroup>>("/topic/tasks/all", (response) => {
        console.log(response)
        dispatch(update(response))
    });

    useWsSubscription<any>("/topic/processer/encode/progress", (response) => {
        console.log(response)
    });

    useEffect(() => {
        client?.publish({
            destination: "/app/tasks/all"
        });
    }, [client, dispatch]);

    return (
        <>
          <Box display="block">
            <Grid container sx={{
              height: 50,
              width: "100%",
              maxHeight: "100%",
              overflow: "hidden",
              display: "flex",
              alignItems: "center",
              backgroundColor: muiTheme.palette.background.paper
            }}>
              <Grid item xs={2}>
                <Typography variant="h6">Tasks</Typography>
              </Grid>
              <Grid item xs={10}>
                <TextField
                    hiddenLabel 
                    placeholder="Search"
                    fullWidth={true}
                    id="search-field"
                    variant="filled"
                    />
              </Grid>
            </Grid>


            <Box sx={{
              display: "block",
              height: "calc(100% - 120px)",
              overflow: "hidden",
              position: "absolute",
              width: "100%"
            }}>
              <SortableGroupedTable items={taskGroups.items ?? []} columns={columns} customizer={createTableCell}  />
            </Box>
          </Box>
    
        </>
    )
}
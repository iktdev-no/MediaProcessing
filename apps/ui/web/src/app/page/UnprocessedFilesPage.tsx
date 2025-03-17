import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useStompClient } from "react-stomp-hooks";
import { Box, Button, Grid, TextField, Typography, useTheme } from '@mui/material';
import { ExplorerItem } from "../../types";
import { ContextMenuItem } from "../features/ContextMenu";
import { RootState } from "../store";
import { useWsSubscription } from "../ws/subscriptions";
import { FileInfo, FileInfoGroup, IncomingUnprocessedFiles, update, } from "../store/unprocessed-files-slice";
import SimpleTable from "../features/table/sortableTable";
import { TablePropetyConfig } from "../features/table/table";
import MultiListSortedTable from "../features/table/multiListSortedTable";


const columns: Array<TablePropetyConfig> = [
  { label: "Name", accessor: "name" },
  { label: "Checksum", accessor: "checksum" },
];

export default function UnprocessedFilesPage() {
    const muiTheme = useTheme();
    const dispatch = useDispatch();
    const client = useStompClient();
    const files = useSelector((state: RootState) => state.unprocessedFiles);
    const [tableItems, setTableItems] = useState<Array<FileInfoGroup>>([]);

    const [selectedRow, setSelectedRow] = useState<ExplorerItem|null>(null);
    const [actionableItems, setActionableItems] = useState<Array<ContextMenuItem>>([]);

    useWsSubscription<IncomingUnprocessedFiles>("/topic/files/unprocessed", (response) => {
        dispatch(update(response))
    });


    const pullData = () => {
      client?.publish({
        destination: "/app/files/unprocessed"
      });
    }

    useEffect(() => {
        client?.publish({
          destination: "/app/files/unprocessed"
        });

        const intervalId = setInterval(pullData, 20000);
        return () => {
          clearInterval(intervalId); // Fjern intervallet når komponenten fjernes fra DOM
      };
    }, [client, dispatch]);

    useEffect(() => {
        const entries = [
          {
            title: "In Process",
            items: files.inProcess
          },
          {
            title: "Available",
            items: files.available
          }
        ];
        setTableItems(entries);
        console.log(entries)
        
    }, [files])

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
                <Typography variant="h6">Unprocessed Files</Typography>
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
              <MultiListSortedTable items={tableItems ?? []} columns={columns}   />
            </Box>
          </Box>
    
        </>
      )
}
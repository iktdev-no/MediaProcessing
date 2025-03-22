import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store";
import { useEffect } from "react";
import { useStompClient } from "react-stomp-hooks";
import { Box, Button, IconButton, Typography, useTheme } from "@mui/material";
import IconRefresh from '@mui/icons-material/Refresh'
import IconCompleted from '@mui/icons-material/Check'
import IconWorking from '@mui/icons-material/Engineering';
import { TablePropetyConfig } from "../features/table/table";
import SimpleTable from "../features/table/sortableTable";

const columns: Array<TablePropetyConfig> = [
    { label: "Title", accessor: "givenTitle" },
    { label: "Type", accessor: "determinedType" },
    { label: "Collection", accessor: "givenCollection" },
    { label: "Encoded", accessor: "eventEncoded" }
];



export default function LaunchPage() {
    const dispatch = useDispatch();
    const muiTheme = useTheme();
    const client = useStompClient();
    /*useEffect(() => {
        if (simpleList.items.filter((item) => item.encodingTimeLeft !== null).length > 0) {
            columns.push({
                label: "Completion",
                accessor: "encodingTimeLeft"
            })
        }
    }, [simpleList, dispatch])*/

    const onRefresh = () => {
        client?.publish({
            "destination": "/app/items",
            "body": "Potato"
        })
    }


    return (
        <Box display="block">
            <Box sx={{
                height: 50,
                width: "100%",
                maxHeight: "100%",
                overflow: "hidden",
                display: "flex",
                alignItems: "center",
                justifyContent: "flex-start",
                backgroundColor: muiTheme.palette.background.paper
            }}>
                <Box sx={{
                    display: "flex",
                }}>
                    <Button
                        startIcon={ <IconRefresh /> }
                        onClick={onRefresh} sx={{
                        borderRadius: 5,
                        textTransform: 'none'
                    }}>Refresh</Button >
                    <Button
                        startIcon={ <IconCompleted /> }
                        onClick={onRefresh} sx={{
                        borderRadius: 5,
                        textTransform: 'none'
                    }}>Completed</Button >
                    <Button
                        startIcon={ <IconWorking /> }
                        onClick={onRefresh} sx={{
                        borderRadius: 5,
                        textTransform: 'none'
                    }}>Working</Button >
                </Box>
            </Box>
            <Box sx={{
                display: "block",
                height: "calc(100% - 120px)",
                overflow: "hidden",
                position: "absolute",
                width: "100%"
            }}>

            </Box>
        </Box>
    )
}
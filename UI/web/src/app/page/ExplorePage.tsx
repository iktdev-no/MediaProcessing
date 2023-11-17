import { useEffect } from 'react';
import { UnixTimestamp } from '../features/UxTc';
import { Box, Button, Typography, useTheme } from '@mui/material';
import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../store';
import SimpleTable, { TableCellCustomizer, TablePropetyConfig, TableRowActionEvents } from '../features/table';
import { useStompClient } from 'react-stomp-hooks';
import { useWsSubscription } from '../ws/subscriptions';
import { updateItems } from '../store/explorer-slice';
import FolderIcon from '@mui/icons-material/Folder';
import IconForward from '@mui/icons-material/ArrowForwardIosRounded';
import IconHome from '@mui/icons-material/Home';
import { ExplorerItem, ExplorerCursor, ExplorerItemType } from '../../types';


const createTableCell: TableCellCustomizer<ExplorerItem> = (accessor, data) => {
  switch (accessor) {
    case "created": {
      if (typeof data[accessor] === "number") {
        const timestampObject = { timestamp: data[accessor] as number }; // Opprett et objekt med riktig struktur
        return UnixTimestamp(timestampObject);
      } else {
        return null;
      }
    }
    case "extension": {
      if (data[accessor] === null) {
        return <FolderIcon sx={{ margin: 1 }} />
      } else {
        return <Typography>{data[accessor]}</Typography>
      }
    }
    default: return null;
  }
};

const columns: Array<TablePropetyConfig> = [
  { label: "Name", accessor: "name" },
  { label: "Format", accessor: "extension" },
  { label: "Created", accessor: "created" },
];



function getPartFor(path: string, index: number): string | null {
  if (path.match(/\//)) {
    return path.split(/\//, index + 1).join("/");
  } else if (path.match(/\\/)) {
    return path.split(/\\/, index + 1).join("\\");
  }
  return null;
}

function getSegmentedNaviagatablePath(navigateTo: (path: string | null) => void, path: string | null): JSX.Element {
  console.log(path);
  const parts: Array<string> = path?.split(/\\|\//).map((value: string, index: number) => value.replaceAll(":", "")) ?? [];
  const segments = parts.map((name: string, index: number) => {
    return (
      <Box key={index} sx={{
        display: "flex",
        flexDirection: "row",
        alignItems: "center"
      }}>
        <Button sx={{
          borderRadius: 5
        }} onClick={() => navigateTo(getPartFor(path!, index))}>
          <Typography>{name}</Typography>
        </Button>
        {index < parts.length - 1 && <IconForward fontSize="small" />}
      </Box>
    )
  });


  console.log(parts)
  return (
    <Box display="flex">
      {segments}
    </Box>
  )
}


export default function ExplorePage() {
  const muiTheme = useTheme();
  const dispatch = useDispatch();
  const client = useStompClient();
  const cursor = useSelector((state: RootState) => state.explorer)

  const navigateTo = (path: string | null) => {
    console.log(path)
    if (path) {
      client?.publish({
        destination: "/app/explorer/navigate",
        body: path
      })
    }
  }

  const onItemSelectedEvent: TableRowActionEvents<ExplorerItem> = {
    click: (row: ExplorerItem) => null,
    doubleClick: (row: ExplorerItem) => {
      console.log(row);
      if (row.type === "FOLDER") {
        navigateTo(row.path);
      }
    },
    contextMenu: (row: ExplorerItem) => null
  }

  const onHomeClick = () => {
    client?.publish({
      destination: "/app/explorer/home"
    })
  }

  useWsSubscription<ExplorerCursor>("/topic/explorer/go", (response) => {
    dispatch(updateItems(response))
  });


  useEffect(() => {
    if (cursor)


    // Kjør din funksjon her når komponenten lastes inn for første gang
    // Sjekk om cursor er null
    if (cursor.path === null && client !== null) {
      console.log(cursor)
      // Kjør din funksjon her når cursor er null og client ikke er null
      client?.publish({
        destination: "/app/explorer/home"
      });

      // Alternativt, du kan dispatche en Redux handling her
      // dispatch(fetchDataAction()); // Eksempel på å dispatche en handling
    }
  }, [cursor, client, dispatch]);


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
          <Button onClick={onHomeClick} sx={{
            borderRadius: 5
          }}>
            <IconHome />
          </Button>
          <Box sx={{
            borderRadius: 5,
            backgroundColor: muiTheme.palette.divider
          }}>
            {getSegmentedNaviagatablePath(navigateTo, cursor?.path)}
          </Box>
        </Box>
      </Box>
      <Box sx={{
        display: "block",
        height: "calc(100% - 120px)",
        overflow: "hidden",
        position: "absolute",
        width: "100%"
      }}>
        <SimpleTable items={cursor?.items ?? []} columns={columns} customizer={createTableCell} onRowClickedEvent={onItemSelectedEvent} />
      </Box>
    </Box>
  )
}

import {
    Box,
    Button,
    CircularProgress,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import type { UiEvent } from "../../types/types";
import { colorFromUuid } from "../../util";

export function EventsTable({
  events,
  loading,
  onShowDetails,
  onShowLineage,
}: {
  events: UiEvent[] | undefined;
  loading: boolean;
  onShowDetails: (ev: UiEvent) => void;
  onShowLineage: (ev: UiEvent) => void;
}) {
  const navigate = useNavigate();

  if (loading && !events) {
    return (
      <Box
        display="flex"
        alignItems="center"
        justifyContent="center"
        height="100%"
      >
        <CircularProgress />
      </Box>
    );
  }

  return (
    <TableContainer
      component={Paper}
      sx={{
        flex: 1,
        minHeight: 0,
        overflow: "auto",
      }}
    >
      <Table stickyHeader size="small">
        <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Reference</TableCell>
            <TableCell>Event ID</TableCell>
            <TableCell>Event</TableCell>
            <TableCell>Persisted</TableCell>
            <TableCell></TableCell>
          </TableRow>
        </TableHead>

        <TableBody>
          {events?.map((ev) => (
            <TableRow key={ev.id} hover>
              <TableCell>{ev.id}</TableCell>
              <TableCell
                sx={{
                  cursor: "pointer",
                  color: colorFromUuid(ev.referenceId),
                  fontWeight: 600,
                }}
                onClick={() => navigate(`/events/sequence/${ev.referenceId}`)}
              >
                {ev.referenceId}
              </TableCell>
              <TableCell>{ev.eventId}</TableCell>
              <TableCell>{ev.event}</TableCell>
              <TableCell>{ev.persistedAt}</TableCell>
              <TableCell>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => onShowDetails(ev)}
                >
                  Vis data
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => onShowLineage(ev)}
                >
                  Vis lineage
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

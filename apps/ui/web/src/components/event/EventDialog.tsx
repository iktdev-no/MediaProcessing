import DeleteIcon from "@mui/icons-material/Delete";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from "@mui/material";
import { toast } from "react-toastify";
import { deleteEvent } from "../../api/coordinator/events";
import type { Response, UiEvent } from "../../types/types";
import { JsonViewer } from "../JsonViewer";

export function EventDialog({
  event,
  open,
  onClose,
}: {
  event: UiEvent | null;
  open: boolean;
  onClose: () => void;
}) {
  if (!event) return null;

  const deleteEventAction = async () => {
    const response: Response = await deleteEvent(
      event.referenceId,
      event.eventId,
    );
    if (response.success) {
      toast.success(`Event ${event.event} deleted`);
      onClose();
    } else if (!response.success) {
      toast.error(`Failed to delete event ${event.event}, ${response.message}`);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xl" fullWidth>
      <DialogTitle>Event {event.eventId}</DialogTitle>

      <DialogContent
        sx={{
          display: "grid",
          gridTemplateColumns: "1fr 2fr",
          gap: 3,
          height: "80vh", // JSON får full høyde
          overflow: "hidden",
        }}
      >
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            gap: 2,
            overflowY: "auto",
          }}
        >
          <Typography>
            <strong>Id:</strong> {event.eventId}
          </Typography>
          <Typography>
            <strong>Reference:</strong> {event.referenceId}
          </Typography>
          <Typography>
            <strong>Event:</strong> {event.event}
          </Typography>
          <Typography>
            <strong>Persisted:</strong> {event.persistedAt}
          </Typography>

          <Box marginTop={"auto"}>
            <Typography variant={"h6"}>Actions</Typography>
            <Stack direction={"row"} spacing={1}>
              <Button
                variant="contained"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={() => deleteEventAction()}
              >
                Delete event
              </Button>
            </Stack>
          </Box>
        </Box>
        <Box
          sx={{
            background: "#111",
            borderRadius: 1,
            p: 2,
            overflowY: "auto",
          }}
        >
          <JsonViewer value={event.data} />
        </Box>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Lukk</Button>
      </DialogActions>
    </Dialog>
  );
}

import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from "@mui/material"
import type { UiEvent } from "../../types/types"
import { JsonViewer } from "../JsonViewer"

export function EventDialog({
    event,
    open,
    onClose
}: {
    event: UiEvent | null
    open: boolean
    onClose: () => void
}) {
    if (!event) return null

    return (
        <Dialog open={open} onClose={onClose} maxWidth="xl" fullWidth>
            <DialogTitle>Event {event.eventId}</DialogTitle>

            <DialogContent
                sx={{
                    display: "grid",
                    gridTemplateColumns: "1fr 2fr",
                    gap: 3,
                    height: "80vh", // JSON får full høyde
                    overflow: "hidden"
                }}
            >
                <Box sx={{ display: "flex", flexDirection: "column", gap: 2, overflowY: "auto" }}>
                    <Typography><strong>ID:</strong> {event.id}</Typography>
                    <Typography><strong>Reference:</strong> {event.referenceId}</Typography>
                    <Typography><strong>Event:</strong> {event.event}</Typography>
                    <Typography><strong>Persisted:</strong> {event.persistedAt}</Typography>
                </Box>
                <Box
                    sx={{
                        background: "#111",
                        borderRadius: 1,
                        p: 2,
                        overflowY: "auto"
                    }}
                >
                    <JsonViewer value={event.data} />
                </Box>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose}>Lukk</Button>
            </DialogActions>
        </Dialog>
    )
}

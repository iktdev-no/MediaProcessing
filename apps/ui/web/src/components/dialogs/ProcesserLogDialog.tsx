import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Typography
} from "@mui/material";
import { useEffect, useRef, useState } from "react";

type ProcesserLogDialogProps = {
    open: boolean;
    onClose: () => void;

    // Kun én av disse skal være satt
    logPath?: string;
    taskId?: string;
};

export function ProcesserLogDialog({ open, onClose, logPath, taskId }: ProcesserLogDialogProps) {
    const [loading, setLoading] = useState(false);
    const [log, setLog] = useState<string>("");
    const [error, setError] = useState<string | null>(null);

    const eventSourceRef = useRef<EventSource | null>(null);

    const isLargeScreen = window.innerHeight > 1080;

    // ------------------------------------------------------------
    // GET log file (static)
    // ------------------------------------------------------------
    const fetchLog = async () => {
        if (!logPath) return;

        setLoading(true);
        setError(null);
        setLog("");

        try {
            const res = await fetch(`/api/processer/logs?path=${encodeURIComponent(logPath)}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const text = await res.text();
            setLog(text);
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    // ------------------------------------------------------------
    // SSE streaming (live logs)
    // ------------------------------------------------------------
    const startSSE = () => {
        if (!taskId) return;

        setLoading(true);
        setError(null);
        setLog("");

        const es = new EventSource(`/api/tasks/${taskId}/log-stream`);
        eventSourceRef.current = es;

        es.onmessage = (event) => {
            setLoading(false);
            setLog((prev) => prev + event.data + "\n");
        };

        es.onerror = () => {
            setError("Mistet kontakt med loggstrømmen");
            es.close();
        };
    };

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------
    useEffect(() => {
        if (!open) {
            // cleanup
            eventSourceRef.current?.close();
            return;
        }

        if (taskId) startSSE();
        else if (logPath) fetchLog();

        return () => {
            eventSourceRef.current?.close();
        };
    }, [open, taskId, logPath]);

    // ------------------------------------------------------------
    // UI
    // ------------------------------------------------------------
    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="lg"
            PaperProps={{
                sx: {
                    width: isLargeScreen ? "80vw" : "100vw",
                    height: isLargeScreen ? "80vh" : "100vh",
                    m: 0,
                    borderRadius: isLargeScreen ? 2 : 0,
                    display: "flex",
                    flexDirection: "column"
                }
            }}
        >
            <DialogTitle>Prosesslogg</DialogTitle>

            <DialogContent
                sx={{
                    flex: 1,
                    overflow: "hidden",
                    display: "flex",
                    flexDirection: "column"
                }}
            >
                {loading && (
                    <Box
                        sx={{
                            flex: 1,
                            display: "flex",
                            justifyContent: "center",
                            alignItems: "center",
                            flexDirection: "column"
                        }}
                    >
                        <CircularProgress />
                        <Typography sx={{ mt: 2 }}>Leser logg…</Typography>
                    </Box>
                )}

                {!loading && error && (
                    <Typography color="error">{error}</Typography>
                )}

                {!loading && !error && (
                    <Box
                        component="pre"
                        sx={{
                            flex: 1,
                            overflowY: "auto",
                            background: "#111",
                            color: "#eee",
                            p: 2,
                            borderRadius: 1,
                            whiteSpace: "pre-wrap"
                        }}
                    >
                        {log}
                    </Box>
                )}
            </DialogContent>

            <DialogActions>
                <Button variant="contained" onClick={onClose}>
                    Lukk
                </Button>
            </DialogActions>
        </Dialog>
    );
}

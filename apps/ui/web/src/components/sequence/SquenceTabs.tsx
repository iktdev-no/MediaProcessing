import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Box, Typography, CircularProgress, Paper, Tabs, Tab, IconButton, Tooltip, Divider, Chip } from "@mui/material";
import ChromeReaderModeIcon from '@mui/icons-material/ChromeReaderMode';
import { getSequence, getSequenceInfo } from "../../api/coordinator/sequence";
import type { SequenceSummary, LifecycleNode, UiEvent, UiTask } from "../../types/types";
import { TaskCard } from "../../components/task/TaskCard";
import { getTasksForReference } from "../../api/coordinator/tasks";
import { getEffectiveEventsHistory } from "../../api/coordinator/events";
import { EventDialog } from "../../components/event/EventDialog";
import { LineageDialog } from "../../components/event/EventLinageDialog";
import { SequenceSummaryCard } from "../../components/sequence/SequenceSummaryCard";

export function EventsTab({ events, onShowDetails, onShowLineage }: {
    events: Array<UiEvent>;
    onShowDetails: (ev: UiEvent) => void;
    onShowLineage: (ev: UiEvent) => void;
}) {
    return (
        <Box sx={{ flex: 1, minHeight: 0, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1.5, pr: 1 }}>
            {events.map((ev) => (
                <Paper
                    key={ev.eventId}
                    variant="outlined"
                    sx={{ p: 2, cursor: "pointer", "&:hover": { backgroundColor: "action.hover" } }}
                    onClick={() => onShowDetails(ev)}
                >
                    <Typography variant="subtitle2" color="primary" fontWeight={600}>{ev.event}</Typography>
                    <Typography variant="caption" color="text.secondary">
                        ID: {ev.eventId} | Tid: {new Date(ev.persistedAt).toLocaleString("no-NO")}
                    </Typography>
                </Paper>
            ))}
        </Box>
    );
}

export function TasksTab({ tasks }: { tasks: Array<UiTask> }) {
    return (
        <Box sx={{ flex: 1, minHeight: 0, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1.5, pr: 1 }}>
            {tasks.map((task) => (
                <TaskCard
                    key={task.taskId}
                    task={task}
                    show="taskId"
                    onCopy={() => navigator.clipboard.writeText(task.taskId)}
                    onReferenceIdClicked={() => { }}
                    onCanceltask={() => { }}
                />
            ))}
        </Box>
    );
}

export function SplitViewContainer({ events, tasks, onShowDetails }: {
    events: Array<UiEvent>;
    tasks: Array<UiTask>;
    onShowDetails: (ev: UiEvent) => void;
    onShowLineage: (ev: UiEvent) => void;
}) {
    return (
        <Box
            sx={{
                flex: 1,
                minHeight: 0,
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 2,
                overflow: "hidden"
            }}
        >
            <Box
                sx={{
                    display: "flex",
                    flexDirection: "column",
                    height: "100%",
                    minHeight: 0,
                    border: "1px solid",
                    borderColor: "divider",
                    borderRadius: 1,
                    p: 2,
                    backgroundColor: "background.paper"
                }}
            >
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2, flexShrink: 0 }}>
                    Events ({events.length})
                </Typography>
                <Box sx={{ flex: 1, minHeight: 0, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1.5, pr: 1 }}>
                    {events.map((ev) => (
                        <Paper
                            key={ev.eventId}
                            variant="outlined"
                            sx={{ p: 1.5, cursor: "pointer", "&:hover": { backgroundColor: "action.hover" } }}
                            onClick={() => onShowDetails(ev)}
                        >
                            <Typography variant="body2" fontWeight={600} color="primary">{ev.event}</Typography>
                            <Typography
                                variant="caption"
                                sx={{ color: "text.secondary", opacity: 0.8, display: "flex", gap: 0.5 }}
                            >
                                {ev.derivedOf?.join(" • ")}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                {new Date(ev.persistedAt).toLocaleTimeString("no-NO")}
                            </Typography>
                        </Paper>
                    ))}
                </Box>
            </Box>

            <Box
                sx={{
                    display: "flex",
                    flexDirection: "column",
                    height: "100%",
                    minHeight: 0,
                    border: "1px solid",
                    borderColor: "divider",
                    borderRadius: 1,
                    p: 2,
                    backgroundColor: "background.paper"
                }}
            >
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2, flexShrink: 0 }}>
                    Tasks ({tasks.length})
                </Typography>
                <Box sx={{ flex: 1, minHeight: 0, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1.5, pr: 1 }}>
                    {tasks.map((task) => (
                        <TaskCard
                            key={task.taskId}
                            task={task}
                            show="taskId"
                            onCopy={() => navigator.clipboard.writeText(task.taskId)}
                            onReferenceIdClicked={() => { }}
                            onCanceltask={() => { }}
                        />
                    ))}
                </Box>
            </Box>
        </Box>
    );
}
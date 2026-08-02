import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Box, Typography, CircularProgress, Paper, Tabs, Tab, IconButton, Tooltip, Divider, Chip } from "@mui/material";
import ChromeReaderModeIcon from '@mui/icons-material/ChromeReaderMode';
import { getSequence, getSequenceInfo } from "../../api/coordinator/sequence";
import type { SequenceSummary, LifecycleNode, UiEvent, UiTask } from "../../types/types";
import { TaskCard } from "../../components/task/TaskCard";
import { getTasksForReference } from "../../api/coordinator/tasks";
import { getEffectiveEventsHistory } from "../../api/coordinator/events";

export default function SequenceViewPage() {
    const { referenceId } = useParams<{ referenceId: string }>();
    const [loading, setLoading] = useState<boolean>(true);
    const [tabIndex, setTabIndex] = useState<number>(0);
    const [splitView, setSplitView] = useState<boolean>(false);

    // Data-states
    const [seqInfo, setSeqInfo] = useState<SequenceSummary | undefined>();
    const [sequence, setSequence] = useState<Array<LifecycleNode>>([]);
    const [events, setEvents] = useState<Array<UiEvent>>([]);
    const [tasks, setTasks] = useState<Array<UiTask>>([]);

    useEffect(() => {
        if (!referenceId) return;
        setLoading(true);

        Promise.all([
            getSequenceInfo(referenceId).catch(() => undefined),
            getSequence(referenceId).catch(() => []),
            getEffectiveEventsHistory(referenceId).catch(() => []),
            getTasksForReference(referenceId).catch(() => []),
        ])
            .then(([seqInfoData, seqData, eventData, taskData]) => {
                setSeqInfo(seqInfoData);
                setSequence(seqData);
                setEvents(eventData);
                setTasks(taskData);
            })
            .finally(() => {
                setLoading(false);
            });
    }, [referenceId]);

    if (loading) {
        return (
            <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box
            sx={{
                p: 3,
                height: "100%",
                display: "flex",
                flexDirection: "column",
                gap: 2,
                mx: "auto",
                boxSizing: "border-box"
            }}
        >
            <PageHeader
                referenceId={referenceId}
                tabIndex={tabIndex}
                splitView={splitView}
                onToggleSplit={() => setSplitView(!splitView)}
            />

            {/* Hovedlayout: Grid med Summary til venstre/topp og innhold til høyre */}
            <Box
                sx={{
                    display: "grid",
                    gridTemplateCode: { xs: "1fr", md: "320px 1fr" },
                    gridTemplateColumns: { xs: "1fr", md: "320px 1fr" },
                    gap: 2,
                    flex: 1,
                    minHeight: 0,
                    overflow: "hidden"
                }}
            >
                {/* Venstre kolonne: Sekvens-sammendrag */}
                <Box sx={{ overflowY: "auto", display: "flex", flexDirection: "column" }}>
                    <SequenceSummaryCard seqInfo={seqInfo} />
                </Box>

                {/* Høyre kolonne: Tabs og Innhold */}
                <Box
                    sx={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 2,
                        minHeight: 0,
                        overflow: "hidden"
                    }}
                >
                    <NavigationTabs
                        tabIndex={tabIndex}
                        sequenceLength={sequence.length}
                        eventsLength={events.length}
                        tasksLength={tasks.length}
                        onChangeTab={(val) => {
                            setTabIndex(val);
                            if (val === 0) setSplitView(false);
                        }}
                    />

                    <Box
                        sx={{
                            flex: 1,
                            minHeight: 0,
                            display: "flex",
                            flexDirection: "column",
                            overflow: "hidden"
                        }}
                    >
                        {tabIndex === 0 && <SequenceTab sequence={sequence} />}
                        {tabIndex > 0 && !splitView && (
                            tabIndex === 1 ? <EventsTab events={events} /> : <TasksTab tasks={tasks} />
                        )}
                        {tabIndex > 0 && splitView && <SplitViewContainer events={events} tasks={tasks} />}
                    </Box>
                </Box>
            </Box>
        </Box>
    );
}

// --- INTERNE DELKOMPONENTER ---

function PageHeader({ referenceId, tabIndex, splitView, onToggleSplit }: {
    referenceId?: string;
    tabIndex: number;
    splitView: boolean;
    onToggleSplit: () => void;
}) {
    return (
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexShrink: 0 }}>
            <Typography variant="h5" fontWeight={600}>
                Sekvens: {referenceId}
            </Typography>

            {tabIndex > 0 && (
                <Tooltip title={splitView ? "Vis enkel visning" : "Sidestill Events & Tasks"}>
                    <IconButton
                        color={splitView ? "primary" : "default"}
                        onClick={onToggleSplit}
                        sx={{ border: '1px solid', borderColor: 'divider' }}
                    >
                        <ChromeReaderModeIcon />
                    </IconButton>
                </Tooltip>
            )}
        </Box>
    );
}

function SequenceSummaryCard({ seqInfo }: { seqInfo?: SequenceSummary }) {
    if (!seqInfo) {
        return (
            <Paper variant="outlined" sx={{ p: 2, height: "100%" }}>
                <Typography variant="subtitle2" color="text.secondary">Ingen sammendrag tilgjengelig.</Typography>
            </Paper>
        );
    }

    return (
        <Paper variant="outlined" sx={{ p: 2.5, display: "flex", flexDirection: "column", gap: 2, height: "100%", boxSizing: "border-box", backgroundColor: "background.paper" }}>
            <Box>
                <Typography variant="subtitle1" fontWeight={700} color="primary">
                    {seqInfo.title || "Ukjent tittel"}
                </Typography>
                {seqInfo.collection && (
                    <Typography variant="body2" color="text.secondary">
                        Kolleksjon: {seqInfo.collection}
                    </Typography>
                )}
            </Box>

            <Divider />

            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Media Type:</Typography>
                    <Chip label={seqInfo.mediaType || "Ukjent"} size="small" color="secondary" variant="outlined" />
                </Box>

                {seqInfo.episodeInfo && (
                    <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Sesong / Episode:</Typography>
                        <Typography variant="body2" fontWeight={500}>
                            S{seqInfo.episodeInfo.seasonNumber}E{seqInfo.episodeInfo.episodeNumber}
                            {seqInfo.episodeInfo.episodeTitle ? ` - ${seqInfo.episodeInfo.episodeTitle}` : ""}
                        </Typography>
                    </Box>
                )}
            </Box>

            {seqInfo.metadata && (
                <>
                    <Divider />
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
                        <Typography variant="caption" fontWeight={600} color="text.secondary">METADATA</Typography>
                        <Typography variant="body2">Kilde: {seqInfo.metadata.source}</Typography>
                        <Typography variant="body2">Har cover: {seqInfo.metadata.hasCover ? "Ja" : "Nei"}</Typography>
                        {seqInfo.metadata.genres.length > 0 && (
                            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5, mt: 0.5 }}>
                                {seqInfo.metadata.genres.map((genre) => (
                                    <Chip key={genre} label={genre} size="small" variant="filled" sx={{ fontSize: '0.75rem' }} />
                                ))}
                            </Box>
                        )}
                    </Box>
                </>
            )}

            {seqInfo.failingReasons && (
                <>
                    <Divider />
                    <Box>
                        <Typography variant="caption" fontWeight={600} color="error">FEIL / TILSTAND</Typography>
                        <Typography variant="body2" color="error" fontWeight={500}>
                            {seqInfo.failingReasons}
                        </Typography>
                    </Box>
                </>
            )}
        </Paper>
    );
}

function NavigationTabs({ tabIndex, sequenceLength, eventsLength, tasksLength, onChangeTab }: {
    tabIndex: number;
    sequenceLength: number;
    eventsLength: number;
    tasksLength: number;
    onChangeTab: (val: number) => void;
}) {
    return (
        <Paper sx={{ flexShrink: 0 }}>
            <Tabs
                value={tabIndex}
                onChange={(_, val) => onChangeTab(val)}
                indicatorColor="primary"
                textColor="primary"
            >
                <Tab label={`Sekvens (${sequenceLength})`} />
                <Tab label={`Events (${eventsLength})`} />
                <Tab label={`Tasks (${tasksLength})`} />
            </Tabs>
        </Paper>
    );
}

function SequenceTab({ sequence }: { sequence: Array<LifecycleNode> }) {
    if (sequence.length === 0) {
        return <Typography color="text.secondary">Ingen noder funnet for denne sekvensen.</Typography>;
    }

    return (
        <Box sx={{ flex: 1, minHeight: 0, overflowY: "auto", display: "flex", flexDirection: "column", gap: 2.5, pr: 1 }}>
            {sequence.map((node) => {
                if (node.type === "EventTaskGroup") {
                    return (
                        <Paper key={node.lifecycleId} variant="outlined" sx={{ p: 2, display: "flex", flexDirection: "column", gap: 1.5 }}>
                            {node.taskOwnerEvent && (
                                <Box>
                                    <Typography variant="subtitle2" color="primary" fontWeight={600}>
                                        {node.taskOwnerEvent.event}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {new Date(node.taskOwnerEvent.persistedAt).toLocaleString("no-NO")}
                                    </Typography>
                                </Box>
                            )}
                            <Divider />
                            <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5, pl: 1 }}>
                                {node.tasks.map((taskItem) => {
                                    if (!taskItem.task) return null;
                                    return (
                                        <TaskCard
                                            key={taskItem.taskId}
                                            task={taskItem.task}
                                            show="taskId"
                                            onCopy={() => navigator.clipboard.writeText(taskItem.taskId)}
                                            onReferenceIdClicked={() => { }}
                                            onCanceltask={() => { }}
                                        />
                                    );
                                })}
                            </Box>
                        </Paper>
                    );
                }
                return (
                    <Paper key={node.lifecycleId} variant="outlined" sx={{ p: 2, backgroundColor: "action.hover" }}>
                        <Typography variant="subtitle2" fontWeight={600}>{node.event?.event}</Typography>
                        <Typography variant="caption" color="text.secondary">
                            {node.event?.persistedAt ? new Date(node.event.persistedAt).toLocaleString("no-NO") : ""}
                        </Typography>
                    </Paper>
                );
            })}
        </Box>
    );
}

function EventsTab({ events }: { events: Array<UiEvent> }) {
    return (
        <Box sx={{ flex: 1, minHeight: 0, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1.5, pr: 1 }}>
            {events.map((ev) => (
                <Paper key={ev.eventId} variant="outlined" sx={{ p: 2 }}>
                    <Typography variant="subtitle2" color="primary" fontWeight={600}>{ev.event}</Typography>
                    <Typography variant="caption" color="text.secondary">
                        ID: {ev.eventId} | Tid: {new Date(ev.persistedAt).toLocaleString("no-NO")}
                    </Typography>
                </Paper>
            ))}
        </Box>
    );
}

function TasksTab({ tasks }: { tasks: Array<UiTask> }) {
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

function SplitViewContainer({ events, tasks }: { events: Array<UiEvent>; tasks: Array<UiTask> }) {
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
                        <Paper key={ev.eventId} variant="outlined" sx={{ p: 1.5 }}>
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
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
import { EventsTab, SplitViewContainer, TasksTab } from "../../components/sequence/SquenceTabs";
import { SequenceTaskCard } from "../../components/sequence/SequenceTaskCard";
import { SequenceEventCard } from "../../components/sequence/SequenceEventCard";
import MoveToInboxOutlinedIcon from '@mui/icons-material/MoveToInboxOutlined';

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

    // Dialog states for Events & Lineage
    const [selectedEvent, setSelectedEvent] = useState<UiEvent | null>(null);
    const [lineageOpen, setLineageOpen] = useState(false);
    const [lineageEvent, setLineageEvent] = useState<UiEvent | null>(null);

    function onShowLineage(ev: UiEvent) {
        setLineageEvent(ev);
        setLineageOpen(true);
    }

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
                            tabIndex === 1 ? (
                                <EventsTab
                                    events={events}
                                    onShowDetails={(ev) => setSelectedEvent(ev)}
                                    onShowLineage={onShowLineage}
                                />
                            ) : (
                                <TasksTab tasks={tasks} />
                            )
                        )}
                        {tabIndex > 0 && splitView && (
                            <SplitViewContainer
                                events={events}
                                tasks={tasks}
                                onShowDetails={(ev) => setSelectedEvent(ev)}
                                onShowLineage={onShowLineage}
                            />
                        )}
                    </Box>
                </Box>
            </Box>

            {/* Dialogs */}
            <EventDialog
                event={selectedEvent}
                open={!!selectedEvent}
                onClose={() => setSelectedEvent(null)}
            />
            <LineageDialog
                open={lineageOpen}
                onClose={() => setLineageOpen(false)}
                referenceId={lineageEvent?.referenceId ?? null}
                selectedEventId={lineageEvent?.eventId ?? null}
            />
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
                                        <Box key={taskItem.taskId} sx={{ display: "flex", flexDirection: "column", gap: 1.5, pl: 2, borderLeft: "2px dashed", borderColor: "divider" }}>
                                            <SequenceTaskCard
                                                task={taskItem.task}
                                                show="taskId"
                                                onCopy={() => navigator.clipboard.writeText(taskItem.taskId)}
                                                onReferenceIdClicked={() => { }}
                                                onCanceltask={() => { }}
                                            />

                                            {taskItem.taskResultEvents && taskItem.taskResultEvents.length > 0 && (
                                                <Box
                                                    sx={{
                                                        display: "flex",
                                                        alignItems: "center",
                                                        my: 0.5,
                                                        color: "text.secondary"
                                                    }}
                                                >
                                                    <Box sx={{ flex: 1, height: "1px", backgroundColor: "divider" }} />
                                                    <Box sx={{ px: 1, display: "flex", alignItems: "center" }}>
                                                        <MoveToInboxOutlinedIcon fontSize="small" />
                                                    </Box>
                                                    <Box sx={{ flex: 1, height: "1px", backgroundColor: "divider" }} />
                                                </Box>
                                            )}

                                            <Box sx={{ ml: 4 }}>
                                                {taskItem.taskResultEvents?.map((ev) => (
                                                    <SequenceEventCard
                                                        key={ev.eventId}
                                                        event={ev}
                                                    />
                                                ))}
                                            </Box>
                                        </Box>
                                    );
                                })}
                            </Box>
                        </Paper>
                    );
                }
                return (
                    <Paper key={node.lifecycleId}>
                        {node.event && (
                            <SequenceEventCard event={node.event} />
                        )}
                    </Paper>
                );
            })}
        </Box>
    );
}


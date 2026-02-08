import {
    Paper,
    Stack,
    Typography
} from "@mui/material"

import AccessTimeIcon from "@mui/icons-material/AccessTime"
import CheckIcon from "@mui/icons-material/Check"
import EventIcon from "@mui/icons-material/Event"
import ListIcon from "@mui/icons-material/List"
import ScheduleIcon from "@mui/icons-material/Schedule"
import UpdateIcon from "@mui/icons-material/Update"
import WhatshotIcon from "@mui/icons-material/Whatshot"
import type { SequenceHealth } from "../../types/transfer-model"
import { formatDurationMs, normalDate, parseDurationMs } from "../../util"


export function OverdueSequenceCard({ seq }: { seq: SequenceHealth }) {
    const ageMs = parseDurationMs(seq.age)
    const overdueMs = parseDurationMs(seq.overdueDuration)

    return (
        <Paper
            elevation={2}
            sx={{
                p: 2,
                border: 1,
                borderColor: seq.isOverdue ? "error.main" : "success.main",
                borderRadius: 2,
            }}
        >
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {seq.referenceId}
            </Typography>

            <Stack spacing={0.5} sx={{ mt: 1 }}>
                <Typography><EventIcon fontSize="small" /> Startet: {normalDate.format(new Date(seq.startTime))}</Typography>
                <Typography><AccessTimeIcon fontSize="small" /> Alder: {formatDurationMs(ageMs)}</Typography>
                <Typography><ScheduleIcon fontSize="small" /> Forventet ferdig: {normalDate.format(new Date(seq.expectedFinishTime))}</Typography>

                {seq.isOverdue ? (
                    <Typography sx={{ color: "error.main", fontWeight: 600 }}>
                        <WhatshotIcon fontSize="small" /> Overdue: {formatDurationMs(overdueMs)}
                    </Typography>
                ) : (
                    <Typography sx={{ color: "success.main" }}>
                        <CheckIcon fontSize="small" /> Innenfor forventet vindu
                    </Typography>
                )}

                <Typography><UpdateIcon fontSize="small" /> Sist event: {normalDate.format(new Date(seq.lastEventAt))}</Typography>
                <Typography><ListIcon fontSize="small" /> Events: {seq.eventCount}</Typography>
            </Stack>
        </Paper>
    )
}


import {
    Paper,
    Stack,
    Typography
} from "@mui/material"

import CheckCircleIcon from "@mui/icons-material/CheckCircle"
import ErrorIcon from "@mui/icons-material/Error"
import WarningIcon from "@mui/icons-material/Warning"

import type { CoordinatorHealthStatus } from "../../types/transfer-model"
import { normalDate } from "../../util"

export function StatusHeader({ status, lastActivity }: { status: CoordinatorHealthStatus, lastActivity: string | null }) {
    const icon =
        status === "HEALTHY" ? <CheckCircleIcon color="success" /> :
            status === "DEGRADED" ? <WarningIcon color="warning" /> :
                <ErrorIcon color="error" />

    const borderColor =
        status === "HEALTHY" ? "success.main" :
            status === "DEGRADED" ? "warning.main" :
                "error.main"

    return (
        <Paper
            elevation={3}
            sx={{
                p: 2,
                borderLeft: 4,
                borderColor,
            }}
        >
            <Stack spacing={1}>
                <Stack direction="row" spacing={1} alignItems="center">
                    {icon}
                    <Typography variant="h6">{status}</Typography>
                </Stack>

                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                    Sist aktivitet: {lastActivity ? normalDate.format(new Date(lastActivity)) : "—"}
                </Typography>
            </Stack>
        </Paper>
    )
}

import AutorenewIcon from "@mui/icons-material/Autorenew"
import CancelIcon from "@mui/icons-material/Cancel"
import CheckCircleIcon from "@mui/icons-material/CheckCircle"
import ErrorIcon from "@mui/icons-material/Error"
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty"
import { keyframes } from "@mui/material"
import type { TaskStatus } from "../../types/backendTypes"

const spin = keyframes({
    from: { transform: "rotate(0deg)" },
    to: { transform: "rotate(360deg)" }
})

export function TaskStatusIcon({ status }: { status: TaskStatus }) {
    switch (status) {
        case "Pending":
            return (
                <HourglassEmptyIcon
                    sx={{ animation: `${spin} 1.5s linear infinite` }}
                    color="warning"
                />
            )

        case "InProgress":
            return (
                <AutorenewIcon
                    sx={{ animation: `${spin} 1.2s linear infinite` }}
                    color="info"
                />
            )

        case "Completed":
            return <CheckCircleIcon color="success" />

        case "Failed":
            return <ErrorIcon color="error" />

        case "Cancelled":
            return <CancelIcon sx={{ color: "#cc00ff" }} /> // neon lilla

        default:
            return null
    }
}

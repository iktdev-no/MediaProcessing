import ReplayIcon from "@mui/icons-material/Replay"
import WarningAmberIcon from "@mui/icons-material/WarningAmber"
import { Box, Button, Typography } from "@mui/material"
import { useState } from "react"
import { toast } from "react-toastify"
import { resetFailedTask } from "../../api/tasks"
import type { UiTask } from "../../types/backendTypes"

export function TaskActions({ task, reload }: { task: UiTask; reload: () => void }) {
    const [forceMode, setForceMode] = useState(false)

    // Task status from backend is "Failed" (capital F)
    if (task.status !== "Failed") return null

    const handleReset = async () => {
        try {
            await resetFailedTask(task.taskId, false, {
                onError: (status) => {
                    if (status === 409) {
                        toast.warning("Force reset required ⚠️")
                        setForceMode(true)
                    }
                }
            })
            toast.success("Task reset successfully ✔️")
            reload()
        } catch (err: any) {
            if (err.status !== 409) {
                toast.error("Reset failed")
            }
        }
    }


    const handleForceReset = async () => {
        try {
            await resetFailedTask(task.taskId, true)
            toast.success("Task force-reset (audit logged) 📝")
            reload()
        } catch {
            toast.error("Force reset failed")
        }
    }

    return (
        <Box sx={{ mt: 3 }}>
            {!forceMode && (
                <Button
                    variant="contained"
                    color="primary"
                    startIcon={<ReplayIcon />}
                    onClick={handleReset}
                >
                    Reset Task
                </Button>
            )}

            {forceMode && (
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                    <Typography variant="body2" color="warning.main">
                        Force reset required
                    </Typography>
                    <Button
                        variant="contained"
                        color="warning"
                        startIcon={<WarningAmberIcon />}
                        onClick={handleForceReset}
                    >
                        Force Reset Task
                    </Button>
                </Box>
            )}
        </Box>
    )
}

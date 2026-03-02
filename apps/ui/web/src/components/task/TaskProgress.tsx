import { Box, LinearProgress, Typography } from "@mui/material";
import type { Progress } from "../../types/transfer-model";
import type { UiTask } from "../../types/types";
import { formatDuration } from "../../utils/timeUtil";

export interface TaskProgressProps {
    task: UiTask,
    progressUpdate: Progress | undefined
}

export function TaskProgress({ task, progressUpdate }: TaskProgressProps) {
    const progress = progressUpdate?.progress ?? task.progress ?? -1;

    // Ikke vis noe hvis vi ikke har progress og task ikke er i progress
    if (progress === -1 && task.status !== "InProgress") {
        return null;
    }

    // Indeterminate når progress mangler
    const isIndeterminate = progress === -1;

    // Ferdig?
    const isDone = task.status === "Completed" || progress >= 100;

    return (
        <Box mt={1}>
            {/* Top row: ETA left, percent right */}
            <Box
                sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center"
                }}
            >
                {/* ETA (kun hvis vi har det) */}
                {(progressUpdate?.type === "EncodeProgress" && progressUpdate?.additionalInfo) ? (
                    <Typography variant="body2">
                        Forventet ferdig om:{" "}
                        {formatDuration(progressUpdate.additionalInfo.estimatedCompletionSeconds)}
                    </Typography>
                ) : (
                    <span /> // holder layouten stabil
                )}

                {/* Prosent helt til høyre */}
                <Typography variant="body2">
                    {isIndeterminate ? "…" : `${progress}%`}
                </Typography>
            </Box>

            {/* Progress bar */}
            <LinearProgress
                variant={isIndeterminate ? "indeterminate" : "determinate"}
                value={isIndeterminate ? undefined : progress}
                color={isDone ? "success" : "primary"}
                sx={{ mt: 1 }}
            />
        </Box>
    );
}



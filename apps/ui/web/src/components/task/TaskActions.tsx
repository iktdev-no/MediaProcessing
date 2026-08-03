import ReplayIcon from "@mui/icons-material/Replay";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { Box, Button, Typography, useTheme } from "@mui/material";
import { useState } from "react";
import { toast } from "react-toastify";
import { patchTaskIgnore, resetFailedTask } from "../../api/coordinator/tasks";
import type { UiTask } from "../../types/types";
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';

export function TaskActions({
  task,
  reload,
}: {
  task: UiTask;
  reload: () => void;
}) {
  const theme = useTheme()
  const [forceMode, setForceMode] = useState(false);

  // Task status from backend is "Failed" (capital F)
  if (task.status! in ["Failed", "Completed"]) return null;

  const handleReset = async () => {
    try {
      await resetFailedTask(task.taskId, false, {
        onError: (status) => {
          if (status === 409) {
            toast.warning("Force reset required ⚠️");
            setForceMode(true);
          }
        },
      });
      toast.success("Task reset successfully ✔️");
      reload();
    } catch (err: any) {
      if (err.status !== 409) {
        toast.error("Reset failed");
      }
    }
  };

  const handleForceReset = async () => {
    try {
      await resetFailedTask(task.taskId, true);
      toast.success("Task force-reset (audit logged) 📝");
      reload();
    } catch {
      toast.error("Force reset failed");
    }
  };

  const handleIgnore = async () => {
    try {
      const response = await patchTaskIgnore(task.taskId)
      if (response.skippedEventId) {
        toast.success("Replaced result with skipp..");
      }
      reload();
    } catch {
      toast.error("Force reset failed");
    }
  }

  return (
    <Box sx={{ mt: 3, display: "flex", flexDirection: "row", gap: 4 }}>
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
      <Button variant="outlined" color="secondary"
        onClick={handleIgnore}
        startIcon={<VisibilityOffIcon />}>
        Ignore
      </Button>
    </Box>
  );
}

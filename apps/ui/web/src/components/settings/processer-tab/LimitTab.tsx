import SpeedIcon from "@mui/icons-material/Speed";
import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  FormControlLabel,
  Slider,
  Stack,
  Switch,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { getCpuLimit, setCpuLimit } from "../../../api/processer/cpuLimit";
import type { CPULimit } from "../../../types/types";

export default function LimitTab() {
  const [cpu, setCpu] = useState<CPULimit | null>(null);
  const [dirty, setDirty] = useState(false);
  const [loading, setLoading] = useState(true);

  // Load CPU limit once
  useEffect(() => {
    getCpuLimit().then((limit) => {
      setCpu(limit);
      setLoading(false);
    });
  }, []);

  function updateCpu(newValue: CPULimit) {
    setCpu(newValue);
    setDirty(true);
  }

  async function applyCpu() {
    if (!cpu) return;
    await setCpuLimit(cpu);
    setDirty(false);
  }

  if (loading || !cpu) return <div>Laster…</div>;

  return (
    <Card
      elevation={3}
      sx={{
        maxWidth: 600,
        borderRadius: 2,
        overflow: "hidden",
      }}
    >
      <CardContent>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <SpeedIcon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h5" fontWeight={600}>
            CPU Limiting
          </Typography>
        </Stack>

        <Typography variant="body2" sx={{ mb: 3, color: "text.secondary" }}>
          Control how much CPU the media processor is allowed to use. Useful for
          keeping the system responsive during heavy workloads.
        </Typography>

        <Divider sx={{ mb: 3 }} />

        <FormControlLabel
          control={
            <Switch
              checked={cpu.enabled}
              onChange={(e) => updateCpu({ ...cpu, enabled: e.target.checked })}
            />
          }
          label={
            <Typography variant="subtitle1" fontWeight={500}>
              Enable CPU limiting
            </Typography>
          }
        />

        {cpu.enabled && (
          <Box sx={{ mt: 4 }}>
            <Stack
              direction="row"
              justifyContent="space-between"
              sx={{ mb: 1 }}
            >
              <Typography variant="subtitle2" fontWeight={600}>
                CPU Usage Cap
              </Typography>

              <Typography variant="subtitle2" color="text.secondary">
                {cpu.limit}%
              </Typography>
            </Stack>

            <Slider
              value={cpu.limit}
              onChange={(_, v) => updateCpu({ ...cpu, limit: v as number })}
              min={1}
              max={100}
              step={1}
              valueLabelDisplay="auto"
              sx={{
                "& .MuiSlider-thumb": {
                  transition: "0.2s",
                },
              }}
            />

            <Typography variant="body2" sx={{ mt: 1, color: "text.secondary" }}>
              {cpu.limit < 30 &&
                "Low limit — system responsiveness prioritized."}
              {cpu.limit >= 30 &&
                cpu.limit < 70 &&
                "Balanced — good performance without hogging the CPU."}
              {cpu.limit >= 70 &&
                "High limit — maximum performance, may affect responsiveness."}
            </Typography>
          </Box>
        )}

        <Stack direction="row" justifyContent="flex-end" sx={{ mt: 4 }}>
          <Button
            variant="contained"
            disabled={!dirty}
            onClick={applyCpu}
            sx={{ px: 4 }}
          >
            Apply
          </Button>
        </Stack>
      </CardContent>
    </Card>
  );
}

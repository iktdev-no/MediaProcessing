import {
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import type {
  CoordinatorPreference,
  Retention,
} from "../../../types/types";
import { FieldSection } from "../../FieldSection";
import { triggerCacheCleanup, triggerCacheWipe, triggerInboxCleanup, triggerInboxWipe } from "../../../api/coordinator/media";
import { ConfirmationDialog } from "../../ConfirmationDialog";
import { useState } from "react";
import { LoadingToast } from "../../LoadingToast";

export default function CleanupRetentionTab({
  prefs,
  setPrefs,
}: {
  prefs: CoordinatorPreference;
  setPrefs: (p: CoordinatorPreference) => void;
}) {
  const [dialog, setDialog] = useState<{
    open: boolean
    title: string
    message: string
    confirmLabel: string
    confirmColor: "primary" | "error" | "warning"
    onConfirm: () => void
    onCancel?: () => void
  } | null>(null)

  const cleanup = prefs.cleanup;

  if (!cleanup) {
    return <Typography>Missing cleanup preferences</Typography>;
  }

  const cache = cleanup.cacheCleanupPreference;
  const input = cleanup.inputCleanupPreference;

  const updateCleanup = (patch: Partial<typeof cleanup>) =>
    setPrefs({
      ...prefs,
      cleanup: { ...cleanup, ...patch },
    });

  const updateCache = (patch: Partial<typeof cache>) =>
    updateCleanup({
      cacheCleanupPreference: { ...cache, ...patch },
    });

  const updateInput = (patch: Partial<typeof input>) =>
    updateCleanup({
      inputCleanupPreference: { ...input, ...patch },
    });

  const updateCacheRetention = (patch: Partial<Retention>) =>
    updateCache({
      retention: { ...cache.retention, ...patch },
    });

  const updateInputRetention = (patch: Partial<Retention>) =>
    updateInput({
      retention: { ...input.retention, ...patch },
    });

  return (
    <>
      <Stack spacing={6}>
        <Typography variant="h5">Cleanup & Retention</Typography>

        {/* Cache Section */}
        <FieldSection
          title="Cache retention & cleanup"
          info="Styrer hvor lenge cachefiler beholdes og om de skal slettes automatisk."
        >
          <Stack
            direction="row"
            alignItems="center"
            justifyContent="space-between"
            sx={{
              backgroundColor: "#222222",
              borderRadius: 5,
              p: 2,
            }}
          >
            <Typography>Enable cache retention & cleanup</Typography>
            <Switch
              checked={cache.enabled}
              onChange={(e) => updateCache({ enabled: e.target.checked })}
            />
          </Stack>

          <FieldSection
            title="Retention time"
            info="Hvor lenge cachefiler skal beholdes før automatisk opprydding."
          >
            <Stack direction="row" spacing={2} alignItems="center">
              <TextField
                label="Value"
                type="number"
                value={cache.retention.value}
                onChange={(e) =>
                  updateCacheRetention({ value: Number(e.target.value) })
                }
                sx={{ maxWidth: 140 }}
              />

              <FormControl sx={{ maxWidth: 140 }}>
                <InputLabel>Unit</InputLabel>
                <Select
                  value={cache.retention.unit}
                  label="Unit"
                  onChange={(e) =>
                    updateCacheRetention({
                      unit: e.target.value as "Hours" | "Days",
                    })
                  }
                >
                  <MenuItem value="Hours">Hours</MenuItem>
                  <MenuItem value="Days">Days</MenuItem>
                </Select>
              </FormControl>
            </Stack>
          </FieldSection>

          <FieldSection
            title="Flows"
            info="Hvilke typer prosesser som skal omfattes av cleanup."
          >
            <FormControl sx={{ maxWidth: 200 }}>
              <InputLabel>Flows</InputLabel>
              <Select
                value={cache.flows}
                label="Flows"
                onChange={(e) => updateCache({ flows: e.target.value })}
              >
                <MenuItem value="Any">Any</MenuItem>
                <MenuItem value="Auto">Auto</MenuItem>
                <MenuItem value="Manual">Manual</MenuItem>
              </Select>
            </FormControl>
          </FieldSection>
        </FieldSection>
        <Box display={"flex"}>
          <Button variant="outlined" onClick={() => triggerCacheCleanup()}>
            Run now
          </Button>
          <Button variant="contained" color="warning" sx={{
            marginLeft: "auto"
          }} onClick={() =>
            setDialog({
              open: true,
              title: "Wipe Caches",
              message: "This operation is not fully destructive and irreversible, any running processes will be impacted.",
              confirmLabel: "Execute Wipe",
              confirmColor: "error",
              onConfirm: () => {
                triggerCacheWipe()
              }
            })
          }>
            Wipe
          </Button>
        </Box>
        <hr />

        {/* Input Section */}
        <FieldSection
          title="Input retention & cleanup"
          info="Styrer hvor lenge inputfiler beholdes og om de skal slettes automatisk."
        >
          <Stack
            direction="row"
            alignItems="center"
            justifyContent="space-between"
            sx={{
              backgroundColor: "#222222",
              borderRadius: 5,
              p: 2,
            }}
          >
            <Typography>Enable input retention & cleanup</Typography>
            <Switch
              checked={input.enabled}
              onChange={(e) => updateInput({ enabled: e.target.checked })}
            />
          </Stack>

          <FieldSection
            title="Retention time"
            info="Hvor lenge inputfiler skal beholdes før automatisk opprydding."
          >
            <Stack direction="row" spacing={2} alignItems="center">
              <TextField
                label="Value"
                type="number"
                value={input.retention.value}
                onChange={(e) =>
                  updateInputRetention({ value: Number(e.target.value) })
                }
                sx={{ maxWidth: 140 }}
              />

              <FormControl sx={{ maxWidth: 140 }}>
                <InputLabel>Unit</InputLabel>
                <Select
                  value={input.retention.unit}
                  label="Unit"
                  onChange={(e) =>
                    updateInputRetention({
                      unit: e.target.value as "Hours" | "Days",
                    })
                  }
                >
                  <MenuItem value="Hours">Hours</MenuItem>
                  <MenuItem value="Days">Days</MenuItem>
                </Select>
              </FormControl>
            </Stack>
          </FieldSection>

          <FieldSection
            title="Flows"
            info="Hvilke typer prosesser som skal omfattes av cleanup."
          >
            <FormControl sx={{ maxWidth: 200 }}>
              <InputLabel>Flows</InputLabel>
              <Select
                value={input.flows}
                label="Flows"
                onChange={(e) => updateInput({ flows: e.target.value })}
              >
                <MenuItem value="Any">Any</MenuItem>
                <MenuItem value="Auto">Auto</MenuItem>
                <MenuItem value="Manual">Manual</MenuItem>
              </Select>
            </FormControl>
          </FieldSection>
        </FieldSection>
        <Box display={"flex"}>
          <Button variant="outlined" onClick={() => triggerInboxCleanup()}>
            Run now
          </Button>
          <Button variant="contained" color="warning" sx={{
            marginLeft: "auto"
          }} onClick={() =>
            setDialog({
              open: true,
              title: "Wipe Inbox",
              message: "This operation is fully destructive and irreversible, any running processes will be impacted as well as source files will be completely removed. \nOnly files selected for preservation will remain.\n\nDO NOT PROCCED IF UNSURE.",
              confirmLabel: "Execute Wipe",
              confirmColor: "error",
              onConfirm: () => {
                triggerInboxWipe()
              }
            })
          }>
            Wipe
          </Button>
        </Box>
        <hr />
      </Stack>

      {dialog && (
        <ConfirmationDialog
          open={dialog.open}
          title={dialog.title}
          message={dialog.message}
          confirmLabel={dialog.confirmLabel}
          confirmColor={dialog.confirmColor}
          onCancel={() => {
            dialog.onCancel?.()
            setDialog(null)
          }}
          onConfirm={() => {
            dialog.onConfirm()
            setDialog(null)
          }}
        />
      )}

    </>
  );
}

import {
    Alert,
    Checkbox,
    FormControlLabel,
    Stack,
    Typography,
} from "@mui/material";
import type { CoordinatorPreference } from "../../../types/transfer-model";
import { FieldSection } from "../../FieldSection";
import { AudioCodecEditor } from "./AudioCodecEditor";

export function AudioTab({
  prefs,
  setPrefs,
}: {
  prefs: CoordinatorPreference;
  setPrefs: (p: CoordinatorPreference) => void;
}) {
  const audioPref = prefs.media?.audioPreference;

  if (!prefs.media || !audioPref) {
    return (
      <Typography variant="subtitle1">
        Missing valid Media preference
      </Typography>
    );
  }

  const updateDefault = (patch: any) =>
    setPrefs({
      ...prefs,
      media: {
        ...prefs.media,
        audioPreference: {
          ...audioPref,
          default: { ...audioPref.default, ...patch },
        },
      },
    });

  const updateExtended = (patch: any) =>
    setPrefs({
      ...prefs,
      media: {
        ...prefs.media,
        audioPreference: {
          ...audioPref,
          extended: audioPref.extended
            ? { ...audioPref.extended, ...patch }
            : { ...patch },
        },
      },
    });

  const toggleExtended = (enabled: boolean) =>
    setPrefs({
      ...prefs,
      media: {
        ...prefs.media,
        audioPreference: {
          ...audioPref,
          extended: enabled ? { ...audioPref.default } : null,
        },
      },
    });

  const extended = audioPref.extended;
  const defaultChannels = audioPref.default.channels ?? 2;
  const extendedChannels = extended?.channels ?? 2;

  return (
    <Stack spacing={4}>
      <Typography variant="h5">Audio Encoding</Typography>

      {/* Default */}
      <FieldSection title="Default Audio">
        <AudioCodecEditor codec={audioPref.default} onChange={updateDefault} />
      </FieldSection>

      {/* Toggle extended */}
      <FormControlLabel
        control={
          <Checkbox
            checked={!!extended}
            onChange={(e) => toggleExtended(e.target.checked)}
          />
        }
        label="Enable Extended Audio Encoding (Surround)"
      />

      {/* Extended */}
      {extended && (
        <FieldSection title="Extended Audio (Surround)">
          <AudioCodecEditor codec={extended} onChange={updateExtended} />

          {extended && extendedChannels < defaultChannels && (
            <Alert severity="warning" sx={{ mt: 1 }}>
              Extended audio har færre kanaler enn default. Dette kan være
              uønsket.
            </Alert>
          )}
        </FieldSection>
      )}
    </Stack>
  );
}

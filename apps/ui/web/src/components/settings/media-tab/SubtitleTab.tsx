import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import {
    FormControl,
    IconButton,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Stack,
    Typography,
} from "@mui/material";

import type {
    CoordinatorPreference,
    SubtitleSelectionMode,
} from "../../../types/transfer-model";

export default function SubtitleTab({
  prefs,
  setPrefs,
}: {
  prefs: CoordinatorPreference;
  setPrefs: (p: CoordinatorPreference) => void;
}) {
  const lang = prefs.language;

  if (!lang) {
    return (
      <Typography variant="subtitle1">
        Missing valid Language preference
      </Typography>
    );
  }

  const update = (patch: Partial<typeof lang>) =>
    setPrefs({ ...prefs, language: { ...lang, ...patch } });

  const moveFormat = (index: number, direction: "up" | "down") => {
    const arr = [...lang.subtitleFormatPriority];
    const target = direction === "up" ? index - 1 : index + 1;

    if (target < 0 || target >= arr.length) return;

    [arr[index], arr[target]] = [arr[target], arr[index]];

    update({ subtitleFormatPriority: arr });
  };

  return (
    <Stack spacing={4}>
      <Typography variant="h5">Subtitle Preferences</Typography>

      {/* Subtitle selection mode */}
      <FormControl fullWidth>
        <InputLabel id="subtitle-mode-label">
          Subtitle Selection Mode
        </InputLabel>
        <Select
          labelId="subtitle-mode-label"
          label="Subtitle Selection Mode"
          value={lang.subtitleSelectionMode}
          onChange={(e) =>
            update({
              subtitleSelectionMode: e.target.value as SubtitleSelectionMode,
            })
          }
        >
          <MenuItem value="DialogueOnly">Dialogue Only</MenuItem>
          <MenuItem value="DialogueAndForced">Dialogue + Forced</MenuItem>
          <MenuItem value="All">All Subtitles</MenuItem>
        </Select>
      </FormControl>

      {/* Subtitle format priority */}
      <Stack spacing={1}>
        <Typography variant="h6">Subtitle Format Priority</Typography>
        <Typography variant="body2">
          Highest priority at the top. These formats are tried in order when
          selecting subtitles.
        </Typography>

        <Stack spacing={1}>
          {lang.subtitleFormatPriority.map((fmt, i) => (
            <Paper
              key={fmt}
              elevation={1}
              sx={{
                padding: "8px 12px",
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
              }}
            >
              <Typography>{fmt}</Typography>

              <Stack direction="row" spacing={1}>
                <IconButton
                  size="small"
                  onClick={() => moveFormat(i, "up")}
                  disabled={i === 0}
                >
                  <ArrowUpwardIcon fontSize="small" />
                </IconButton>

                <IconButton
                  size="small"
                  onClick={() => moveFormat(i, "down")}
                  disabled={i === lang.subtitleFormatPriority.length - 1}
                >
                  <ArrowDownwardIcon fontSize="small" />
                </IconButton>
              </Stack>
            </Paper>
          ))}
        </Stack>
      </Stack>
    </Stack>
  );
}

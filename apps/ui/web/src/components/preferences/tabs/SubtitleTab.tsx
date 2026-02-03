import { FormControl, InputLabel, MenuItem, Select, Stack, Typography } from "@mui/material";
import type { PreferenceConfig, SubtitleSelectionMode } from "../../../types/transfer-model";

export function SubtitleTab({
    prefs,
    setPrefs
}: {
    prefs: PreferenceConfig;
    setPrefs: (p: PreferenceConfig) => void;
}) {
    const lang = prefs.language;

    if (!lang) {
        return (<Typography variant="subtitle1">Missing valid Language preference</Typography>)
    }

    const update = (patch: Partial<typeof lang>) =>
        setPrefs({ ...prefs, language: { ...lang, ...patch } });

    return (
        <Stack spacing={3}>
            <Typography variant="h5">Subtitle Preferences</Typography>

            <FormControl fullWidth>
                <InputLabel id="subtitle-mode-label">Subtitle Selection Mode</InputLabel>
                <Select
                    labelId="subtitle-mode-label"
                    label="Subtitle Selection Mode"
                    value={lang.subtitleSelectionMode}
                    onChange={e =>
                        update({
                            subtitleSelectionMode: e.target.value as SubtitleSelectionMode
                        })
                    }
                >
                    <MenuItem value="DialogueOnly">Dialogue Only</MenuItem>
                    <MenuItem value="DialogueAndForced">Dialogue + Forced</MenuItem>
                    <MenuItem value="All">All Subtitles</MenuItem>
                </Select>
            </FormControl>
        </Stack>
    );
}

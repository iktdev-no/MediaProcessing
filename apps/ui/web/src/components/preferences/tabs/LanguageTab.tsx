import {
    Checkbox,
    Chip,
    FormControlLabel,
    List,
    ListItemButton,
    ListItemText,
    Paper,
    Stack,
    TextField,
    Typography
} from "@mui/material";

import { useState } from "react";
import type { PreferenceConfig } from "../../../types/transfer-model";
import { LANGUAGE_OPTIONS } from "../../../utils/languageList";
import { LanguageFlag } from "../../../utils/languageToFlag";

export function LanguageTab({
    prefs,
    setPrefs
}: {
    prefs: PreferenceConfig;
    setPrefs: (p: PreferenceConfig) => void;
}) {
    const lang = prefs.language;

    // Null-safe arrays
    const preferredAudio = lang.preferredAudio ?? [];
    const preferredSubtitles = lang.preferredSubtitles ?? [];

    const update = (patch: Partial<typeof lang>) =>
        setPrefs({ ...prefs, language: { ...lang, ...patch } });

    // Local search state
    const [audioSearch, setAudioSearch] = useState("");
    const [subSearch, setSubSearch] = useState("");

    // Filter suggestions (no ISO3 shown)
    const audioSuggestions = LANGUAGE_OPTIONS.filter(
        (opt) =>
            (opt.label.toLowerCase().includes(audioSearch.toLowerCase()) ||
                opt.code.includes(audioSearch.toLowerCase())) &&
            !preferredAudio.includes(opt.code)
    );

    const subSuggestions = LANGUAGE_OPTIONS.filter(
        (opt) =>
            (opt.label.toLowerCase().includes(subSearch.toLowerCase()) ||
                opt.code.includes(subSearch.toLowerCase())) &&
            !preferredSubtitles.includes(opt.code)
    );

    const addAudio = (code: string) =>
        update({ preferredAudio: [...preferredAudio, code] });

    const addSubtitle = (code: string) =>
        update({ preferredSubtitles: [...preferredSubtitles, code] });

    const removeAudio = (code: string) =>
        update({
            preferredAudio: preferredAudio.filter((c) => c !== code)
        });

    const removeSubtitle = (code: string) =>
        update({
            preferredSubtitles: preferredSubtitles.filter((c) => c !== code)
        });

    return (
        <Stack spacing={4}>
            <Typography variant="h5">Language Preferences</Typography>

            {/* Preferred Audio */}
            <Stack spacing={1}>
                <TextField
                    fullWidth
                    label="Add Preferred Audio Language"
                    value={audioSearch}
                    onChange={(e) => setAudioSearch(e.target.value)}
                />

                {audioSearch.length > 0 && audioSuggestions.length > 0 && (
                    <Paper sx={{ maxHeight: 200, overflowY: "auto" }}>
                        <List dense>
                            {audioSuggestions.map((opt) => (
                                <ListItemButton
                                    key={opt.code}
                                    onClick={() => {
                                        addAudio(opt.code);
                                        setAudioSearch("");
                                    }}
                                >
                                    <ListItemText
                                        primary={
                                            <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                                                <LanguageFlag lang={opt.code} />
                                                {opt.label}
                                            </span>
                                        }
                                    />
                                </ListItemButton>
                            ))}
                        </List>
                    </Paper>
                )}

                {/* Chips */}
                <Stack direction="row" spacing={1} flexWrap="wrap">
                    {preferredAudio.map((code) => {
                        const opt = LANGUAGE_OPTIONS.find((o) => o.code === code);
                        return (
                            <Chip
                                key={code}
                                onDelete={() => removeAudio(code)}
                                label={
                                    <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                        <LanguageFlag lang={code} />
                                        {opt?.label ?? ""}
                                    </span>
                                }
                            />
                        );
                    })}
                </Stack>
            </Stack>

            {/* Preferred Subtitles */}
            <Stack spacing={1}>
                <TextField
                    fullWidth
                    label="Add Preferred Subtitle Language"
                    value={subSearch}
                    onChange={(e) => setSubSearch(e.target.value)}
                />

                {subSearch.length > 0 && subSuggestions.length > 0 && (
                    <Paper sx={{ maxHeight: 200, overflowY: "auto" }}>
                        <List dense>
                            {subSuggestions.map((opt) => (
                                <ListItemButton
                                    key={opt.code}
                                    onClick={() => {
                                        addSubtitle(opt.code);
                                        setSubSearch("");
                                    }}
                                >
                                    <ListItemText
                                        primary={
                                            <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                                                <LanguageFlag lang={opt.code} />
                                                {opt.label}
                                            </span>
                                        }
                                    />
                                </ListItemButton>
                            ))}
                        </List>
                    </Paper>
                )}

                {/* Chips */}
                <Stack direction="row" spacing={1} flexWrap="wrap">
                    {preferredSubtitles.map((code) => {
                        const opt = LANGUAGE_OPTIONS.find((o) => o.code === code);
                        return (
                            <Chip
                                key={code}
                                onDelete={() => removeSubtitle(code)}
                                label={
                                    <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                        <LanguageFlag lang={code} />
                                        {opt?.label ?? ""}
                                    </span>
                                }
                            />
                        );
                    })}
                </Stack>
            </Stack>

            {/* Toggles */}
            <FormControlLabel
                control={
                    <Checkbox
                        checked={lang.preferOriginal ?? false}
                        onChange={(e) => update({ preferOriginal: e.target.checked })}
                    />
                }
                label="Prefer Original Language"
            />

            <FormControlLabel
                control={
                    <Checkbox
                        checked={lang.avoidDub ?? false}
                        onChange={(e) => update({ avoidDub: e.target.checked })}
                    />
                }
                label="Avoid Dubbed Audio"
            />
        </Stack>
    );
}

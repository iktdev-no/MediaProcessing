import {
    Autocomplete,
    Checkbox,
    Chip,
    FormControlLabel,
    Stack,
    TextField,
    Typography
} from "@mui/material";

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
        setPrefs({
            ...prefs,
            language: { ...lang, ...patch }
        });

    return (
        <Stack spacing={3}>
            <Typography variant="h5">Language Preferences</Typography>

            {/* Preferred Audio */}
            <div>
                <Autocomplete
                    multiple
                    options={LANGUAGE_OPTIONS}
                    getOptionLabel={(opt) => `${opt.label} (${opt.code})`}
                    value={LANGUAGE_OPTIONS.filter((o) =>
                        preferredAudio.includes(o.code)
                    )}
                    onChange={(_, values) =>
                        update({
                            preferredAudio: values.map((v) => v.code)
                        })
                    }
                    renderTags={(value, getTagProps) =>
                        value.map((option, index) => (
                            <Chip
                                {...getTagProps({ index })}
                                key={option.code}
                                label={
                                    <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                        <LanguageFlag lang={option.code} />
                                        {option.label}
                                    </span>
                                }
                            />
                        ))
                    }
                    renderInput={(params) => (
                        <TextField {...params} label="Preferred Audio Languages" />
                    )}
                />

                {/* Flag row */}
                <Stack direction="row" spacing={2} mt={1}>
                    {preferredAudio.map((code) => (
                        <LanguageFlag key={code} lang={code} />
                    ))}
                </Stack>
            </div>

            {/* Preferred Subtitles */}
            <div>
                <Autocomplete
                    multiple
                    options={LANGUAGE_OPTIONS}
                    getOptionLabel={(opt) => `${opt.label} (${opt.code})`}
                    value={LANGUAGE_OPTIONS.filter((o) =>
                        preferredSubtitles.includes(o.code)
                    )}
                    onChange={(_, values) =>
                        update({
                            preferredSubtitles: values.map((v) => v.code)
                        })
                    }
                    renderTags={(value, getTagProps) =>
                        value.map((option, index) => (
                            <Chip
                                {...getTagProps({ index })}
                                key={option.code}
                                label={
                                    <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                                        <LanguageFlag lang={option.code} />
                                        {option.label}
                                    </span>
                                }
                            />
                        ))
                    }
                    renderInput={(params) => (
                        <TextField {...params} label="Preferred Subtitle Languages" />
                    )}
                />

                <Stack direction="row" spacing={2} mt={1}>
                    {preferredSubtitles.map((code) => (
                        <LanguageFlag key={code} lang={code} />
                    ))}
                </Stack>
            </div>

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

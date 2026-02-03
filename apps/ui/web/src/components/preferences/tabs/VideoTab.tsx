import type {
    H264Profiles,
    PreferenceConfig,
    Presets,
    VideoCodecType
} from "../../../types/transfer-model";

import {
    Checkbox,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography
} from "@mui/material";

export function VideoTab({
    prefs,
    setPrefs
}: {
    prefs: PreferenceConfig;
    setPrefs: (p: PreferenceConfig) => void;
}) {
    const video = prefs.processer.videoPreference?.codec;
    const enforceMkv = prefs.processer.videoPreference?.enforceMkv ?? false;

    if (!video) {
        return <Typography>No video preferences configured.</Typography>;
    }

    const update = (patch: Partial<typeof video>) =>
        setPrefs({
            ...prefs,
            processer: {
                ...prefs.processer,
                videoPreference: {
                    ...prefs.processer.videoPreference!,
                    codec: { ...video, ...patch }
                }
            }
        });

    const updateContainer = (patch: Partial<typeof prefs.processer.videoPreference>) =>
        setPrefs({
            ...prefs,
            processer: {
                ...prefs.processer,
                videoPreference: {
                    ...prefs.processer.videoPreference!,
                    ...patch
                }
            }
        });

    return (
        <Stack spacing={3}>
            <Typography variant="h5">Video Encoding</Typography>

            {/* Codec Type */}
            <FormControl fullWidth>
                <InputLabel id="video-codec-label">Codec</InputLabel>
                <Select
                    labelId="video-codec-label"
                    label="Codec"
                    value={video.type}
                    onChange={e => update({ type: e.target.value as VideoCodecType })}
                >
                    {[
                        "HEVC",
                        "H264",
                        "VP9",
                        "VP8",
                        "AV1",
                        "VVC",
                        "XVID",
                        "RAW",
                        "COPY"
                    ].map(c => (
                        <MenuItem key={c} value={c}>
                            {c}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>

            {/* CRF */}
            <TextField
                fullWidth
                type="number"
                label="CRF"
                value={video.crf ?? ""}
                onChange={e =>
                    update({ crf: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* Bitrate */}
            <TextField
                fullWidth
                type="number"
                label="Bitrate (kbps)"
                value={video.bitrate ?? ""}
                onChange={e =>
                    update({ bitrate: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* Preset */}
            <FormControl fullWidth>
                <InputLabel id="preset-label">Preset</InputLabel>
                <Select
                    labelId="preset-label"
                    label="Preset"
                    value={video.preset ?? ""}
                    onChange={e => update({ preset: e.target.value as Presets })}
                >
                    {[
                        "Ultrafast",
                        "Superfast",
                        "Veryfast",
                        "Faster",
                        "Fast",
                        "Medium",
                        "Slow",
                        "Slower",
                        "Veryslow",
                        "Placebo"
                    ].map(p => (
                        <MenuItem key={p} value={p}>
                            {p}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>

            {/* H264 Profile */}
            {video.type === "H264" && (
                <FormControl fullWidth>
                    <InputLabel id="profile-label">H.264 Profile</InputLabel>
                    <Select
                        labelId="profile-label"
                        label="H.264 Profile"
                        value={video.profile ?? ""}
                        onChange={e =>
                            update({ profile: e.target.value as H264Profiles })
                        }
                    >
                        {["Baseline", "Main", "High", "High10", "High422", "High444"].map(
                            p => (
                                <MenuItem key={p} value={p}>
                                    {p}
                                </MenuItem>
                            )
                        )}
                    </Select>
                </FormControl>
            )}

            {/* Level */}
            <TextField
                fullWidth
                type="number"
                label="Level"
                value={video.level ?? ""}
                onChange={e =>
                    update({ level: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* QScale */}
            <TextField
                fullWidth
                type="number"
                label="QScale"
                value={video.qscale ?? ""}
                onChange={e =>
                    update({ qscale: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* Tune */}
            <TextField
                fullWidth
                label="Tune"
                value={video.tune ?? ""}
                onChange={e => update({ tune: e.target.value || null })}
            />

            {/* Enforce MKV */}
            <FormControlLabel
                control={
                    <Checkbox
                        checked={enforceMkv}
                        onChange={e =>
                            updateContainer({ enforceMkv: e.target.checked })
                        }
                    />
                }
                label="Enforce MKV Container"
            />
        </Stack>
    );
}

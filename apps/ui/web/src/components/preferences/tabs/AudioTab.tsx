import type {
    AacProfile,
    AudioCodecType,
    OpusApplication,
    PreferenceConfig
} from "../../../types/transfer-model";

import {
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography
} from "@mui/material";

export function AudioTab({
    prefs,
    setPrefs
}: {
    prefs: PreferenceConfig;
    setPrefs: (p: PreferenceConfig) => void;
}) {

    if (!prefs.processer || !prefs.processer.audioPreference) {
        return (<Typography variant="subtitle1">Missing valid Processer preference</Typography>)
    }

    const audioCodec = prefs.processer.audioPreference?.codec;

    if (!audioCodec) {
        return <Typography>No audio preferences configured.</Typography>;
    }

    const update = (patch: Partial<typeof audioCodec>) =>
        setPrefs({
            ...prefs,
            processer: {
                ...prefs.processer,
                audioPreference: {
                    codec: { ...audioCodec, ...patch }
                }
            }
        });

    return (
        <Stack spacing={3}>
            <Typography variant="h5">Audio Encoding</Typography>

            {/* Codec Type */}
            <FormControl fullWidth>
                <InputLabel id="audio-codec-label">Codec</InputLabel>
                <Select
                    labelId="audio-codec-label"
                    label="Codec"
                    value={audioCodec.type}
                    onChange={e => update({ type: e.target.value as AudioCodecType })}
                >
                    {[
                        "AAC",
                        "MP3",
                        "OPUS",
                        "VORBIS",
                        "FLAC",
                        "AC3",
                        "EAC3",
                        "DTS",
                        "PCM",
                        "COPY"
                    ].map(c => (
                        <MenuItem key={c} value={c}>
                            {c}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>

            {/* Bitrate */}
            <TextField
                fullWidth
                type="number"
                label="Bitrate (kbps)"
                value={audioCodec.bitrate ?? ""}
                onChange={e =>
                    update({ bitrate: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* Channels */}
            <TextField
                fullWidth
                type="number"
                label="Channels"
                value={audioCodec.channels ?? ""}
                onChange={e =>
                    update({ channels: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* Sample Rate */}
            <TextField
                fullWidth
                type="number"
                label="Sample Rate (Hz)"
                value={audioCodec.sampleRate ?? ""}
                onChange={e =>
                    update({ sampleRate: e.target.value ? Number(e.target.value) : null })
                }
            />

            {/* AAC Profile */}
            {audioCodec.type === "AAC" && (
                <FormControl fullWidth>
                    <InputLabel id="aac-profile-label">AAC Profile</InputLabel>
                    <Select
                        labelId="aac-profile-label"
                        label="AAC Profile"
                        value={audioCodec.profile ?? ""}
                        onChange={e =>
                            update({ profile: e.target.value as AacProfile })
                        }
                    >
                        <MenuItem value="LC">LC</MenuItem>
                        <MenuItem value="HE">HE</MenuItem>
                        <MenuItem value="HEv2">HEv2</MenuItem>
                    </Select>
                </FormControl>
            )}

            {/* Opus Application */}
            {audioCodec.type === "OPUS" && (
                <FormControl fullWidth>
                    <InputLabel id="opus-app-label">Opus Application</InputLabel>
                    <Select
                        labelId="opus-app-label"
                        label="Opus Application"
                        value={audioCodec.application ?? ""}
                        onChange={e =>
                            update({ application: e.target.value as OpusApplication })
                        }
                    >
                        <MenuItem value="Audio">Audio</MenuItem>
                        <MenuItem value="Voip">Voip</MenuItem>
                        <MenuItem value="LowDelay">Low Delay</MenuItem>
                    </Select>
                </FormControl>
            )}

            {/* Compression Level */}
            <TextField
                fullWidth
                type="number"
                label="Compression Level"
                value={audioCodec.compressionLevel ?? ""}
                onChange={e =>
                    update({
                        compressionLevel: e.target.value ? Number(e.target.value) : null
                    })
                }
            />
        </Stack>
    );
}

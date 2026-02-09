import {
    Alert,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField
} from "@mui/material";
import type {
    AacProfile,
    AudioCodecConfig,
    AudioCodecType,
    OpusApplication
} from "../../../types/transfer-model";
import { FieldSection } from "../../FieldSection";
import { ChannelSlider } from "./ChannelSlider";

// Maks kanaler per codec-type
const MAX_CHANNELS: Record<AudioCodecType, number> = {
    AAC: 6,
    MP3: 2,
    OPUS: 8,
    VORBIS: 2,
    FLAC: 8,
    AC3: 6,
    EAC3: 16,
    DTS: 8,
    PCM: 8,
    COPY: 32
};

export function AudioCodecEditor({
    codec,
    onChange
}: {
    codec: AudioCodecConfig;
    onChange: (patch: Partial<AudioCodecConfig>) => void;
}) {
    const update = (patch: Partial<AudioCodecConfig>) =>
        onChange({ ...patch });

    const maxChannels = MAX_CHANNELS[codec.type];
    const currentChannels = codec.channels ?? 2;

    // Special handling for COPY: hide channels + force 32
    if (codec.type === "COPY" && currentChannels !== 32) {
        update({ channels: 32 });
    }

    // Auto-clamp channels when codec changes (except COPY)
    if (codec.type !== "COPY" && currentChannels > maxChannels) {
        update({ channels: maxChannels });
    }

    const incompatibleChannels =
        codec.type !== "COPY" && currentChannels > maxChannels;

    return (
        <Stack spacing={4}>

            {/* Codec */}
            <FieldSection
                title="Codec"
                info="Velg hvilken lydkodek som skal brukes. COPY beholder original lyd uten endringer."
            >
                <FormControl fullWidth>
                    <InputLabel>Codec</InputLabel>
                    <Select
                        value={codec.type}
                        label="Codec"
                        onChange={(e) =>
                            update({ type: e.target.value as AudioCodecType })
                        }
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
                        ].map((c) => (
                            <MenuItem key={c} value={c}>
                                {c}
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
            </FieldSection>

            {/* Everything below is hidden when COPY */}
            {codec.type !== "COPY" && (
                <>
                    {/* Bitrate */}
                    <FieldSection
                        title="Bitrate (kbps)"
                        info="Bitrate bestemmer lydkvalitet og filstørrelse. Systemet nedskalerer automatisk hvis kilden har lavere bitrate."
                    >
                        <TextField
                            fullWidth
                            type="number"
                            value={codec.bitrate ?? ""}
                            onChange={(e) =>
                                update({
                                    bitrate: e.target.value
                                        ? Number(e.target.value)
                                        : null
                                })
                            }
                        />
                    </FieldSection>

                    {/* Channels */}
                    <FieldSection
                        title="Channels"
                        info="Antall kanaler i output-lyden. Verdien kan ikke overstige det codec-en støtter og justeres automatisk ned ved behov."
                    >
                        <ChannelSlider
                            value={Math.min(currentChannels, maxChannels)}
                            max={maxChannels}
                            onChange={(v) =>
                                update({ channels: Math.min(v, maxChannels) })
                            }
                        />

                        {incompatibleChannels && (
                            <Alert severity="warning" sx={{ mt: 1 }}>
                                {codec.type} støtter maks {maxChannels} kanaler. Verdien er
                                justert ned.
                            </Alert>
                        )}
                    </FieldSection>

                    {/* Sample Rate */}
                    <FieldSection
                        title="Sample Rate (Hz)"
                        info="Sample rate bestemmer hvor mange ganger per sekund lyden samples. Systemet nedskalerer automatisk hvis kilden har lavere sample rate."
                    >
                        <TextField
                            fullWidth
                            type="number"
                            value={codec.sampleRate ?? ""}
                            onChange={(e) =>
                                update({
                                    sampleRate: e.target.value
                                        ? Number(e.target.value)
                                        : null
                                })
                            }
                        />
                    </FieldSection>

                    {/* AAC Profile */}
                    {codec.type === "AAC" && (
                        <FieldSection
                            title="AAC Profile"
                            info="Velg AAC-profil. LC gir best kompatibilitet, mens HE/HEv2 gir bedre komprimering ved lav bitrate."
                        >
                            <FormControl fullWidth>
                                <InputLabel>AAC Profile</InputLabel>
                                <Select
                                    value={codec.profile ?? ""}
                                    label="AAC Profile"
                                    onChange={(e) =>
                                        update({
                                            profile: e.target.value as AacProfile
                                        })
                                    }
                                >
                                    <MenuItem value="LC">LC</MenuItem>
                                    <MenuItem value="HE">HE</MenuItem>
                                    <MenuItem value="HEv2">HEv2</MenuItem>
                                </Select>
                            </FormControl>
                        </FieldSection>
                    )}

                    {/* Opus Application */}
                    {codec.type === "OPUS" && (
                        <FieldSection
                            title="Opus Application"
                            info="Velg optimaliseringsmodus for Opus. 'Audio' for musikk, 'Voip' for tale, og 'LowDelay' for lav latency."
                        >
                            <FormControl fullWidth>
                                <InputLabel>Opus Application</InputLabel>
                                <Select
                                    value={codec.application ?? ""}
                                    label="Opus Application"
                                    onChange={(e) =>
                                        update({
                                            application: e.target.value as OpusApplication
                                        })
                                    }
                                >
                                    <MenuItem value="Audio">Audio</MenuItem>
                                    <MenuItem value="Voip">Voip</MenuItem>
                                    <MenuItem value="LowDelay">Low Delay</MenuItem>
                                </Select>
                            </FormControl>
                        </FieldSection>
                    )}

                    {/* Compression Level — only for FLAC */}
                    {codec.type === "FLAC" && (
                        <FieldSection
                            title="Compression Level"
                            info="Gjelder kun FLAC. Høyere nivå gir bedre komprimering, men bruker mer CPU under encoding."
                        >
                            <TextField
                                fullWidth
                                type="number"
                                value={codec.compressionLevel ?? ""}
                                onChange={(e) =>
                                    update({
                                        compressionLevel: e.target.value
                                            ? Number(e.target.value)
                                            : null
                                    })
                                }
                            />
                        </FieldSection>
                    )}
                </>
            )}
        </Stack>
    )

}

import type {
  CoordinatorPreference,
  H264Profiles,
  Presets,
  VideoCodecType,
} from "../../../types/types";

import {
  Checkbox,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Slider,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { FieldSection } from "../../FieldSection";

export default function VideoTab({
  prefs,
  setPrefs,
}: {
  prefs: CoordinatorPreference;
  setPrefs: (p: CoordinatorPreference) => void;
}) {
  if (!prefs.media || !prefs.media.videoPreference) {
    return (
      <Typography variant="subtitle1">
        Missing valid Media preference
      </Typography>
    );
  }

  const LEVELS: Record<VideoCodecType, string[]> = {
    H264: [
      "1",
      "1.1",
      "1.2",
      "1.3",
      "2",
      "2.1",
      "2.2",
      "3",
      "3.1",
      "3.2",
      "4",
      "4.1",
      "4.2",
      "5",
      "5.1",
      "5.2",
      "6",
      "6.1",
      "6.2",
    ],
    HEVC: [
      "1",
      "2",
      "2.1",
      "3",
      "3.1",
      "4",
      "4.1",
      "5",
      "5.1",
      "5.2",
      "6",
      "6.1",
      "6.2",
    ],
    VP9: [],
    VP8: [],
    AV1: [],
    VVC: [],
    XVID: [],
    RAW: [],
    COPY: [],
  };

  const QSCALE_CODECS: VideoCodecType[] = ["XVID"];

  const video = prefs.media.videoPreference.codec;
  const enforceMkv = prefs.media.videoPreference.enforceMkv ?? false;

  const update = (patch: Partial<typeof video>) =>
    setPrefs({
      ...prefs,
      media: {
        ...prefs.media,
        videoPreference: {
          ...prefs.media.videoPreference!,
          codec: { ...video, ...patch },
        },
      },
    });

  const updateContainer = (
    patch: Partial<typeof prefs.media.videoPreference>,
  ) =>
    setPrefs({
      ...prefs,
      media: {
        ...prefs.media,
        videoPreference: {
          ...prefs.media.videoPreference!,
          ...patch,
        },
      },
    });

  return (
    <Stack spacing={4}>
      <Typography variant="h5">Video Encoding</Typography>

      {/* Codec */}
      <FieldSection title="Codec">
        <FormControl fullWidth>
          <InputLabel id="video-codec-label">Codec</InputLabel>
          <Select
            labelId="video-codec-label"
            label="Codec"
            value={video.type}
            onChange={(e) => update({ type: e.target.value as VideoCodecType })}
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
              "COPY",
            ].map((c) => (
              <MenuItem key={c} value={c}>
                {c}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </FieldSection>

      {/* CRF */}
      <FieldSection
        title="CRF"
        info={
          video.type === "H264"
            ? "H.264 CRF: 0–51 (18–23 anbefalt).\nLavere = høyere kvalitet."
            : video.type === "HEVC"
              ? "H.265 CRF: 0–51 (20–28 anbefalt).\nLavere = høyere kvalitet."
              : video.type === "VP9"
                ? "VP9 CRF: 0–63 (30–40 anbefalt).\nLavere = høyere kvalitet."
                : video.type === "AV1"
                  ? "AV1 CRF: 0–63 (28–40 anbefalt).\nLavere = høyere kvalitet."
                  : undefined
        }
      >
        <Stack direction="row" spacing={2} alignItems="center">
          <Slider
            min={0}
            max={video.type === "H264" || video.type === "HEVC" ? 51 : 63}
            value={video.crf ?? 23}
            onChange={(_, v) => update({ crf: v as number })}
            sx={{ flexGrow: 1 }}
          />
          <TextField
            type="number"
            value={video.crf ?? ""}
            onChange={(e) =>
              update({ crf: e.target.value ? Number(e.target.value) : null })
            }
            sx={{ width: 80 }}
          />
        </Stack>
      </FieldSection>

      {/* Bitrate */}
      <FieldSection
        title="Bitrate (kbps)"
        info={
          "Bitrate styrer filstørrelse direkte.\n" +
          "Eksempler:\n" +
          "• 1500 kbps – YouTube-lignende kvalitet\n" +
          "• 4000 kbps – god 1080p\n" +
          "• 8000 kbps – høy kvalitet\n" +
          "• 20000+ kbps – visuelt tapsfri"
        }
      >
        <TextField
          fullWidth
          type="number"
          value={video.bitrate ?? ""}
          onChange={(e) =>
            update({ bitrate: e.target.value ? Number(e.target.value) : null })
          }
        />
      </FieldSection>

      {/* Preset */}
      <FieldSection
        title="Preset"
        info={
          "Preset styrer hvor mye tid encoder bruker.\n" +
          "Slower = bedre kvalitet, men tregere encoding."
        }
      >
        <FormControl fullWidth>
          <InputLabel id="preset-label">Preset</InputLabel>
          <Select
            labelId="preset-label"
            label="Preset"
            value={video.preset ?? ""}
            onChange={(e) => update({ preset: e.target.value as Presets })}
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
              "Placebo",
            ].map((p) => (
              <MenuItem key={p} value={p}>
                {p}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </FieldSection>

      {/* H264 Profile */}
      {video.type === "H264" && (
        <FieldSection title="H.264 Profile">
          <FormControl fullWidth>
            <InputLabel id="profile-label">H.264 Profile</InputLabel>
            <Select
              labelId="profile-label"
              label="H.264 Profile"
              value={video.profile ?? ""}
              onChange={(e) =>
                update({ profile: e.target.value as H264Profiles })
              }
            >
              {["Baseline", "Main", "High", "High10", "High422", "High444"].map(
                (p) => (
                  <MenuItem key={p} value={p}>
                    {p}
                  </MenuItem>
                ),
              )}
            </Select>
          </FormControl>
        </FieldSection>
      )}

      {/* Level */}
      {LEVELS[video.type].length > 0 && (
        <FieldSection
          title="Level"
          info={
            "Level begrenser oppløsning og framerate.\n" +
            "Eksempler:\n" +
            "• 4.0 – 1080p 30fps\n" +
            "• 4.1 – 1080p 60fps\n" +
            "• 5.1 – 4K 30fps"
          }
        >
          <FormControl fullWidth>
            <InputLabel id="level-label">Level</InputLabel>
            <Select
              labelId="level-label"
              label="Level"
              value={video.level ?? ""}
              onChange={(e) => update({ level: e.target.value })}
            >
              {LEVELS[video.type].map((lvl) => (
                <MenuItem key={lvl} value={lvl}>
                  {lvl}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </FieldSection>
      )}

      {/* QScale */}
      {QSCALE_CODECS.includes(video.type) && (
        <FieldSection
          title="QScale"
          info={
            "QScale brukes for MPEG‑4/XVID.\n" +
            "Lavere verdi = høyere kvalitet.\n" +
            "Typisk område: 2–5."
          }
        >
          <TextField
            fullWidth
            type="number"
            value={video.qscale ?? ""}
            onChange={(e) =>
              update({ qscale: e.target.value ? Number(e.target.value) : null })
            }
          />
        </FieldSection>
      )}

      {/* Tune */}
      <FieldSection
        title="Tune"
        info={
          "Tune justerer encoder for spesifikke typer innhold.\n" +
          "Eksempler:\n" +
          "• film – bedre for filmgrain\n" +
          "• animation – bedre for anime\n" +
          "• grain – bevarer filmkorn\n" +
          "• fastdecode – enklere å dekode"
        }
      >
        <TextField
          fullWidth
          value={video.tune ?? ""}
          onChange={(e) => update({ tune: e.target.value || null })}
        />
      </FieldSection>

      {/* Enforce MKV */}
      <FieldSection>
        <FormControlLabel
          control={
            <Checkbox
              checked={enforceMkv}
              onChange={(e) =>
                updateContainer({ enforceMkv: e.target.checked })
              }
            />
          }
          label="Enforce MKV Container"
        />
      </FieldSection>
    </Stack>
  );
}

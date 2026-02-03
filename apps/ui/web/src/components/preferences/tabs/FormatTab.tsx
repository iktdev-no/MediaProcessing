import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import { IconButton, Paper, Stack, Typography } from "@mui/material";
import type { PreferenceConfig } from "../../../types/transfer-model";

export function FormatTab({
    prefs,
    setPrefs
}: {
    prefs: PreferenceConfig;
    setPrefs: (p: PreferenceConfig) => void;
}) {
    const lang = prefs.language;

    const move = (index: number, direction: "up" | "down") => {
        const arr = [...lang.subtitleFormatPriority];
        const target = direction === "up" ? index - 1 : index + 1;

        if (target < 0 || target >= arr.length) return;

        [arr[index], arr[target]] = [arr[target], arr[index]];

        setPrefs({
            ...prefs,
            language: { ...lang, subtitleFormatPriority: arr }
        });
    };

    return (
        <Stack spacing={3}>
            <Typography variant="h5">Subtitle Format Priority</Typography>

            <Typography variant="body2">
                Highest priority at the top. These formats are tried in order when selecting subtitles.
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
                            justifyContent: "space-between"
                        }}
                    >
                        <Typography>{fmt}</Typography>

                        <Stack direction="row" spacing={1}>
                            <IconButton
                                size="small"
                                onClick={() => move(i, "up")}
                                disabled={i === 0}
                            >
                                <ArrowUpwardIcon fontSize="small" />
                            </IconButton>

                            <IconButton
                                size="small"
                                onClick={() => move(i, "down")}
                                disabled={i === lang.subtitleFormatPriority.length - 1}
                            >
                                <ArrowDownwardIcon fontSize="small" />
                            </IconButton>
                        </Stack>
                    </Paper>
                ))}
            </Stack>
        </Stack>
    );
}

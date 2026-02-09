import { Box, Slider } from "@mui/material";

const CHANNEL_STEPS = [
    { value: 1, label: "Mono" },
    { value: 2, label: "Stereo" },
    { value: 6, label: "5.1" },
    { value: 8, label: "7.1" }
];

export function ChannelSlider({
    value,
    max = 8,
    onChange
}: {
    value: number;
    max?: number;
    onChange: (v: number) => void;
}) {
    // Filter out marks above max
    const marks = CHANNEL_STEPS.filter(m => m.value <= max);

    return (
        <Box>
            <Slider
                value={value}
                min={1}
                max={max}
                step={null}
                marks={marks}
                onChange={(_, v) => onChange(v as number)}
            />
        </Box>
    );
}

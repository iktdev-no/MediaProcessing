import { Stack, Typography } from "@mui/material";
import { InfoText } from "./InfoText";

export function FieldSection({
    title,
    info,
    children
}: {
    title?: string;
    info?: string | React.ReactNode;
    children: React.ReactNode;
}) {
    return (
        <Stack spacing={1.2}>
            {title && (
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                    {title}
                </Typography>
            )}

            {children}

            {info && <InfoText>{info}</InfoText>}
        </Stack>
    );
}

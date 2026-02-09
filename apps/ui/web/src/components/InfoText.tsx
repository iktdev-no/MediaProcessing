import { Typography } from "@mui/material";

export function InfoText({ children }: { children: React.ReactNode }) {
    return (
        <Typography
            variant="body2"
            color="text.secondary"
            sx={{ whiteSpace: "pre-line" }}
        >
            {children}
        </Typography>
    );
}

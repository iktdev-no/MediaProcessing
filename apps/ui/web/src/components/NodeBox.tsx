import { Box, Typography } from "@mui/material"

export function NodeBox({ icon, label, healthy }: any) {
    return (
        <Box
            sx={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                p: 2,
                borderRadius: 2,
                border: "1px solid",
                borderColor: healthy ? "success.main" : "error.main",
                minWidth: 120,
            }}
        >
            {icon}
            <Typography variant="subtitle1" sx={{ mt: 1 }}>
                {label}
            </Typography>
        </Box>
    )
}

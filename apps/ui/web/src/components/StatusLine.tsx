import { Box } from "@mui/material"

export function StatusLine({ ok }: { ok: boolean }) {
    return (
        <Box
            sx={{
                height: 4,
                marginTop: 1,
                marginBottom: 1,
                width: "100%",
                backgroundColor: ok ? "success.main" : "error.main",
                transition: "background-color 0.3s",
                borderRadius: 1,
            }}
        />
    )
}

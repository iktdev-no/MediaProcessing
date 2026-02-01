import { Alert, Snackbar } from "@mui/material"

export interface LoadingToastProps {
    open: boolean
}

export function LoadingToast({ open }: LoadingToastProps) {
    return (
        <Snackbar
            open={open}
            anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        >
            <Alert
                severity="info"
                variant="filled"
                sx={{ display: "flex", alignItems: "center" }}
            >
                Laster innhold…
            </Alert>
        </Snackbar>
    )
}

import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Typography
} from "@mui/material"

export interface GenericConfirmDialogProps {
    open: boolean
    title: string
    message: string
    confirmLabel?: string
    cancelLabel?: string
    cancelVariant?: "text" | "outlined" | "contained"
    confirmVariant?: "text" | "outlined" | "contained"
    confirmColor?: "primary" | "error" | "warning" | "success" | "info"
    onConfirm: () => void
    onCancel: () => void
}

export function ConfirmationDialog({
    open,
    title,
    message,
    confirmLabel = "OK",
    cancelLabel = "Avbryt",
    confirmColor = "primary",
    cancelVariant = "text",
    confirmVariant = "contained",
    onConfirm,
    onCancel
}: GenericConfirmDialogProps) {
    return (
        <Dialog open={open} onClose={onCancel}   PaperProps={{
    sx: { padding: 2 }
  }}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <Typography sx={{ whiteSpace: "pre-line" }}>{message}</Typography>
            </DialogContent>
            <DialogActions>
                <Button variant={cancelVariant} onClick={onCancel}>{cancelLabel}</Button>
                <Button variant={confirmVariant} color={confirmColor} onClick={onConfirm}>
                    {confirmLabel}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

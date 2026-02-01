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
    onConfirm,
    onCancel
}: GenericConfirmDialogProps) {
    return (
        <Dialog open={open} onClose={onCancel}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <Typography>{message}</Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onCancel}>{cancelLabel}</Button>
                <Button color={confirmColor} onClick={onConfirm}>
                    {confirmLabel}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

import { useCallback, useState } from "react"

export function useToast() {
    const [open, setOpen] = useState(false)
    const [message, setMessage] = useState("")

    const showToast = useCallback((msg: string) => {
        setMessage(msg)
        setOpen(true)
    }, [])

    const handleClose = () => setOpen(false)

    return { open, message, showToast, handleClose }
}

import { Box, Typography } from "@mui/material"


import { useEffect, useState } from "react"
import { continueSequence, getActiveSequences } from "../api/sequence"
import { SequenceRow } from "../components/sequence/SequenceRow"

import { useNavigate } from "react-router-dom"
import { toast } from "react-toastify"
import type { SequenceSummary } from "../types/transfer-model"


export function SequencePage() {
    const navigate = useNavigate();
    const [sequences, setSequences] = useState<SequenceSummary[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        setLoading(true)
        getActiveSequences()
            .then(setSequences)
            .finally(() => setLoading(false))
    }, [])

    const onContinue = async (refId: string) => {
        try {
            await continueSequence(refId)
            const updated = await getActiveSequences()
            setSequences(updated)
            toast.success("Action accepted!")
        } catch (err: any) {
            toast.error(err.message);
        }
    }

    const onNavigateToSequence = (referenceId: string) => {
        navigate(`/events/sequence/${referenceId}`)
    }

    const onDelete = async (refId: string) => {
        try {
            await fetch(`/api/sequences/${refId}/delete`, { method: "POST" })
            const updated = await getActiveSequences()
            setSequences(updated)
            toast.success("Sequence deleted!")
        } catch (err: any) {
            toast.error(err.message);
        }
    }

    return (
        <Box sx={{ m: 2, height: "100%", display: "flex", flexDirection: "column", gap: 2 }}>
            <Typography variant="h5" gutterBottom>
                Active Sequences
            </Typography>

            <Box
                sx={{
                    flex: 1,
                    minHeight: 0,
                    overflow: "auto",
                    pb: 5,
                }}>
                {sequences.map(seq => (
                    <SequenceRow key={seq.referenceId} seq={seq} onContinue={onContinue} onDelete={onDelete} onOpenSequence={onNavigateToSequence} />
                ))}
            </Box>
        </Box>

    )
}


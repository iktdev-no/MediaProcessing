import { Box, Typography } from "@mui/material"


import { useEffect, useState } from "react"
import { continueSequence, getActiveSequences } from "../api/sequence"
import { SequenceRow } from "../components/sequence/SequenceRow"
import type { SequenceSummary } from "../types/backendTypes"

import { toast } from "react-toastify"


export function SequencePage() {
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
        <Box sx={{ p: 2 }}>
            <Typography variant="h5" gutterBottom>
                Active Sequences
            </Typography>

            {sequences.map(seq => (
                <SequenceRow key={seq.referenceId} seq={seq} onContinue={onContinue} onDelete={onDelete} />
            ))}
        </Box>

    )
}


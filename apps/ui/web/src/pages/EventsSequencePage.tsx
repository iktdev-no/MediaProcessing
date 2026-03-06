

import { Box, Paper, Typography } from "@mui/material"
import { useEffect, useState } from "react"
import { useParams } from "react-router-dom"
import { getEffectiveEventsHistory } from "../api/events"
import { EventDialog } from "../components/event/EventDialog"
import { EventsTable } from "../components/event/EventsTable"
import type { UiEvent } from "../types/types"

export default function EventSequencePage() {
    const { referenceId } = useParams<{ referenceId: string }>()
    const [effective, setEffective] = useState<UiEvent[]>([])
    const [loading, setLoading] = useState(true)
    const [selected, setSelected] = useState<UiEvent | null>(null)


    useEffect(() => {
        if (!referenceId) return

        setLoading(true)
        getEffectiveEventsHistory(referenceId)
            .then(events => {
                setEffective(events)
            })
            .finally(() => setLoading(false))
    }, [referenceId])

    return (
        <Box sx={{ height: "100%", display: "flex", flexDirection: "column", }}>
            <Typography variant="h5" gutterBottom>
                Event sequence for {referenceId}
            </Typography>

            <Typography variant="h6" sx={{ mt: 3 }}>
                Effective history (system view)
            </Typography>
            <Paper sx={{ mt: 1, overflowY: 'scroll', mb: 2 }}>
                <EventsTable
                    events={effective}
                    loading={loading}
                    onShowDetails={ev => setSelected(ev)}
                    onShowLineage={ev => { }}
                />
            </Paper>
            <EventDialog
                event={selected}
                open={!!selected}
                onClose={() => setSelected(null)}
            />
        </Box>
    )
}

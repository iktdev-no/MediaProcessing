import {
    Paper,
    Typography
} from "@mui/material"
import Grid from "@mui/material/Grid"

import FlashOnIcon from "@mui/icons-material/FlashOn"
import TrendingUpIcon from "@mui/icons-material/TrendingUp"
import type { EventRate } from "../../types/transfer-model"


export function EventRatePanel({ rate }: { rate: EventRate | null }) {
    if (!rate) return null

    return (
        <Grid container spacing={2} sx={{ mt: 2 }}>
            <Grid size={{ xs: 6 }}>
                <Paper sx={{ p: 2, border: 1, borderColor: "info.main" }}>
                    <Typography><FlashOnIcon /> Events siste minutt</Typography>
                    <Typography variant="h4">{rate.lastMinute}</Typography>
                </Paper>
            </Grid>

            <Grid size={{ xs: 6 }}>
                <Paper sx={{ p: 2, border: 1, borderColor: "primary.main" }}>
                    <Typography><TrendingUpIcon /> Events siste 5 min</Typography>
                    <Typography variant="h4">{rate.lastFiveMinutes}</Typography>
                </Paper>
            </Grid>
        </Grid>
    )
}

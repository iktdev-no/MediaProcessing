import { Box, Typography, Paper, Divider, Chip, Button } from "@mui/material";
import type { SequenceActions, SequenceSummary } from "../../types/types";
import { SquenceActionbutton } from "./SequenceActionButton";


interface SequenceSummaryCardProps {
    seqInfo?: SequenceSummary;
    onActionClick: (action: SequenceActions) => void;
}

export function SequenceSummaryCard({ seqInfo, onActionClick }: SequenceSummaryCardProps) {
    if (!seqInfo) {
        return (
            <Paper variant="outlined" sx={{ p: 2, height: "100%" }}>
                <Typography variant="subtitle2" color="text.secondary">Ingen sammendrag tilgjengelig.</Typography>
            </Paper>
        );
    }

    return (
        <Paper variant="outlined" sx={{ p: 2.5, display: "flex", flexDirection: "column", gap: 2, height: "100%", boxSizing: "border-box", backgroundColor: "background.paper" }}>
            <Box>
                <Typography variant="subtitle1" fontWeight={700} color="primary">
                    {seqInfo.title || "Ukjent tittel"}
                </Typography>
                {seqInfo.collection && (
                    <Typography variant="body2" color="text.secondary">
                        Kolleksjon: {seqInfo.collection}
                    </Typography>
                )}
            </Box>

            <Divider />

            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                    <Typography variant="body2" color="text.secondary">Media Type:</Typography>
                    <Chip label={seqInfo.mediaType || "Ukjent"} size="small" color="secondary" variant="outlined" />
                </Box>

                {seqInfo.episodeInfo && (
                    <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                        <Typography variant="body2" color="text.secondary">Sesong / Episode:</Typography>
                        <Typography variant="body2" fontWeight={500}>
                            S{seqInfo.episodeInfo.seasonNumber}E{seqInfo.episodeInfo.episodeNumber}
                            {seqInfo.episodeInfo.episodeTitle ? ` - ${seqInfo.episodeInfo.episodeTitle}` : ""}
                        </Typography>
                    </Box>
                )}
            </Box>

            {seqInfo.metadata && (
                <>
                    <Divider />
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
                        <Typography variant="caption" fontWeight={600} color="text.secondary">METADATA</Typography>
                        <Typography variant="body2">Kilde: {seqInfo.metadata.source}</Typography>
                        <Typography variant="body2">Har cover: {seqInfo.metadata.hasCover ? "Ja" : "Nei"}</Typography>
                        {seqInfo.metadata.genres.length > 0 && (
                            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5, mt: 0.5 }}>
                                {seqInfo.metadata.genres.map((genre) => (
                                    <Chip key={genre} label={genre} size="small" variant="filled" sx={{ fontSize: '0.75rem' }} />
                                ))}
                            </Box>
                        )}
                    </Box>
                </>
            )}

            {seqInfo.failingReasons && (
                <>
                    <Divider />
                    <Box>
                        <Typography variant="caption" fontWeight={600} color="error">FEIL / TILSTAND</Typography>
                        <Typography variant="body2" color="error" fontWeight={500}>
                            {seqInfo.failingReasons}
                        </Typography>
                    </Box>
                </>
            )}
            <Box sx={{ gap: 1, display: "flex", flexDirection: "column" }}>
                {seqInfo.availableActions.map((action, i) => {
                    return <SquenceActionbutton key={i} action={action} onClick={() => onActionClick?.(action)} />
                })}
            </Box>
        </Paper>
    );
}


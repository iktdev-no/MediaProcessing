import { Box, Card, CardContent, Typography, IconButton, Tooltip, Chip } from "@mui/material";
import type { Sequence, SequenceActions, SequenceSummary } from "../../types/types";
import { colorFromUuid } from "../../util";
import NotStartedIcon from '@mui/icons-material/NotStarted';
import PlayCircleIcon from '@mui/icons-material/PlayCircle';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DeleteIcon from '@mui/icons-material/Delete';
import { SquenceActionbutton } from "./SequenceActionButton";
import { MediaTypeColor } from "./SequenceUtils";

interface Props {
    sequence: Sequence;
    onNavigate: (refId: string) => void;
    onActionClick: (action: SequenceActions) => void;
}

export function SequenceOverviewCard({ sequence, onNavigate, onActionClick }: Props) {


    return (
        <Card
            variant="elevation"
            sx={{
                flexGrow: 1,                    // <--- Gjør at kortet strekker seg for å fylle tomrommet!
                flexBasis: "320px",             // <--- Ideell startbredde før den strekker seg
                minWidth: "300px",
                maxWidth: "450px",              // <--- Hindrer at den blir latterlig bred hvis det er få kort
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                transition: "transform 0.2s, box-shadow 0.2s",
                '&:hover': {
                    transform: "translateY(-2px)",
                    boxShadow: 4,
                }
            }}
        >
            <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1.5, pb: "16px !important" }}>
                {/* Topp-seksjon: ID og Status-ikon */}
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <Typography
                        variant="subtitle2"
                        sx={{
                            color: colorFromUuid(sequence.referenceId),
                            fontWeight: 700,
                            cursor: 'pointer',
                            '&:hover': { textDecoration: 'underline' }
                        }}
                        onClick={() => onNavigate(sequence.referenceId)}
                    >
                        #{sequence.referenceId.split("-")[0]}
                    </Typography>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                        {sequence.mediaType && (
                            <Chip label={sequence.mediaType} size="small" color={MediaTypeColor(sequence.mediaType)} variant="outlined" />
                        )}
                        <Chip
                            label={sequence.mode}
                            size="small"
                            color={sequence.mode === "Auto" ? "success" : "default"}
                            variant="outlined"
                        />
                        <Tooltip title={sequence.mode === "Auto" ? "Auto mode" : "Manual mode"}>
                            {sequence.mode === "Auto" ? (
                                <PlayCircleIcon color="success" fontSize="small" />
                            ) : (
                                <NotStartedIcon color="disabled" fontSize="small" />
                            )}
                        </Tooltip>
                    </Box>
                </Box>

                {/* Tittel og filnavn */}
                <Box>
                    <Typography variant="caption" sx={{ lineHeight: 1.2 }} noWrap>
                        {sequence.collection}
                    </Typography>
                    <Typography variant="h6" sx={{ fontSize: '1rem', fontWeight: 600, lineHeight: 1.2 }} noWrap>
                        {sequence.title || "Uten tittel"}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" noWrap>
                        {sequence.inputFileName || "Ingen fil spesifisert"}
                    </Typography>
                </Box>

                {/* Status for feil eller tilstand */}
                {sequence.hasErrors && (
                    <Chip label="Har feil" color="error" size="small" sx={{ alignSelf: 'flex-start' }} />
                )}

                {/* Handlingsknapper nederst på kortet */}
                <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1, mt: 1, borderTop: '1px solid', borderColor: 'divider', pt: 1 }}>
                    {sequence.availableActions.map((action, i) => {
                        return <SquenceActionbutton key={i} action={action} variant="icon" onClick={() => onActionClick?.(action)} />
                    })}
                </Box>
            </CardContent>
        </Card>
    );
}
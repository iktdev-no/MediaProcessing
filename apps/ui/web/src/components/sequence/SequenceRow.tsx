
import ArticleIcon from '@mui/icons-material/Article';
import BookIcon from '@mui/icons-material/Book';
import ContentCutIcon from '@mui/icons-material/ContentCut';
import DeleteIcon from "@mui/icons-material/Delete";
import ImageIcon from '@mui/icons-material/Image';
import MoveToInboxIcon from '@mui/icons-material/MoveToInbox';
import MovieIcon from '@mui/icons-material/Movie';
import PlayLessonIcon from '@mui/icons-material/PlayLesson';
import TransformIcon from '@mui/icons-material/Transform';
import { Box, Button, IconButton, Paper, Stack, Typography } from "@mui/material";
import type { SequenceSummary } from '../../types/transfer-model';
import { colorFromUuid } from "../../util";
import { ModeIcon } from "./ModeIcon";
import { StateIcon } from "./StateIcon";
import { TaskPipelineItem } from "./TaskPipelineItem";

export function SequencePipeline({ seq }: { seq: SequenceSummary }) {
    return (
        <Stack direction="row" spacing={2} sx={{ mt: 1, overflowX: "auto" }}>
            <TaskPipelineItem icon={<PlayLessonIcon />} status={seq.readStreamsTaskStatus} />
            <TaskPipelineItem icon={<ArticleIcon />} status={seq.metadataTaskStatus} />
            <TaskPipelineItem icon={<MovieIcon />} status={seq.encodeTaskStatus} />
            <TaskPipelineItem icon={<ContentCutIcon />} status={seq.extractTaskStatus} />
            <TaskPipelineItem icon={<TransformIcon />} status={seq.convertTaskStatus} />
            <TaskPipelineItem icon={<ImageIcon />} status={seq.coverDownloadTaskStatus} />
            <TaskPipelineItem icon={<MoveToInboxIcon />} status={seq.contentMigratedTaskStatus} />
            <TaskPipelineItem icon={<BookIcon />} status={seq.contentStoredTaskStatus} />
        </Stack>
    )
}


export function SequenceRow({
    seq,
    onContinue,
    onDelete,
    onOpenSequence
}: {
    seq: SequenceSummary
    onContinue: (refId: string) => void
    onDelete: (refId: string) => void
    onOpenSequence: (refId: string) => void
}) {

    return (
        <Paper
            sx={{
                p: 2,
                mb: 2,
                width: "100%",
                background: "#0d0d0d",
                border: `1px solid ${colorFromUuid(seq.referenceId)}55`,
                borderRadius: 2
            }}
        >
            {/* Header */}
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Typography
                    variant="subtitle1"
                    sx={{ color: colorFromUuid(seq.referenceId), fontWeight: 700, cursor: 'pointer' }}
                    onClick={() => onOpenSequence(seq.referenceId)}
                >
                    {seq.referenceId}
                </Typography>

                <Stack direction="row" spacing={1} alignItems="center">
                    <ModeIcon mode={seq.mode} />
                    <StateIcon state={seq.currentState} />

                    {/* Delete button */}
                    <IconButton
                        size="small"
                        onClick={() => onDelete(seq.referenceId)}
                        sx={{
                            color: "#f55",
                            "&:hover": { color: "#faa" }
                        }}
                    >
                        <DeleteIcon fontSize="small" />
                    </IconButton>
                </Stack>
            </Box>

            {/* Title + input */}
            <Typography variant="body1">{seq.title}</Typography>
            <Typography variant="body2" sx={{ opacity: 0.6 }}>
                {seq.inputFileName ?? "No input file"}
            </Typography>

            {/* Pipeline */}
            <SequencePipeline seq={seq} />

            {/* Continue button */}
            {(seq.mode === "Manual" && seq.currentState === "OnHold") && (
                <Button
                    variant="contained"
                    size="small"
                    sx={{ mt: 2 }}
                    onClick={() => onContinue(seq.referenceId)}
                >
                    Continue
                </Button>
            )}
        </Paper>
    )
}


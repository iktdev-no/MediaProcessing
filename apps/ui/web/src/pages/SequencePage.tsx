import { Box, Typography, CircularProgress } from "@mui/material";
import { useEffect, useState } from "react";
import {
  continueSequence,
  getActiveSequences,
} from "../api/coordinator/sequence";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useTitle } from "../features/useTitle";
import type { SequenceSummary } from "../types/transfer-model";
import { SequenceOverviewCard } from "../components/sequence/SequenceOverviewCard";

export function SequencePage() {
  const navigate = useNavigate();
  const [sequences, setSequences] = useState<SequenceSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Sequences");
  }, [setTitle]);

  const fetchSequences = () => {
    getActiveSequences()
      .then(setSequences)
      .catch((err) => toast.error(err.message || "Kunne ikke hente sekvenser"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    setLoading(true);
    fetchSequences();
  }, []);

  const onContinue = async (refId: string) => {
    try {
      await continueSequence(refId);
      fetchSequences();
      toast.success("Action accepted!");
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const onNavigateToSequence = (referenceId: string) => {
    navigate(`/events/sequence/${referenceId}`);
  };

  const onDelete = async (refId: string) => {
    try {
      await fetch(`/api/sequences/${refId}/delete`, { method: "POST" });
      fetchSequences();
      toast.success("Sequence deleted!");
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  return (
    <Box
      sx={{
        m: 3,
        height: "100%",
        display: "flex",
        flexDirection: "column",
        gap: 2,
      }}
    >
      <Typography variant="h5" gutterBottom fontWeight={600}>
        Active Sequences
      </Typography>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : sequences.length === 0 ? (
        <Typography color="text.secondary">Ingen aktive sekvenser funnet.</Typography>
      ) : (
        <Box
          sx={{
            flex: 1,
            minHeight: 0,
            overflow: "auto",
            pb: 5,
            display: "flex",
            flexWrap: "wrap",
            gap: 2.5,
            alignContent: "flex-start",
          }}
        >
          {sequences.map((seq) => (
            <SequenceOverviewCard
              key={seq.referenceId}
              sequence={seq}
              onNavigate={onNavigateToSequence}
              onContinue={onContinue}
              onDelete={onDelete}
            />
          ))}
        </Box>
      )}
    </Box>
  );
}
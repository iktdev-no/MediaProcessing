import { Box, Typography, CircularProgress } from "@mui/material";
import { useEffect, useState, useCallback } from "react";
import {
  continueSequence,
  getActiveSequences,
} from "../api/coordinator/sequence";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useTitle } from "../features/useTitle";
import type { Sequence, SequenceActions } from "../types/types";
import { SequenceOverviewCard } from "../components/sequence/SequenceOverviewCard";

export function SequencePage() {
  const navigate = useNavigate();
  const [sequences, setSequences] = useState<Sequence[]>([]);
  const [loading, setLoading] = useState(true);

  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Sequences");
  }, [setTitle]);

  // Bruker useCallback slik at funksjonen holder seg stabil
  const fetchSequences = useCallback((showLoader = false) => {
    if (showLoader) setLoading(true);

    getActiveSequences()
      .then(setSequences)
      .catch((err) => toast.error(err.message || "Kunne ikke hente sekvenser"))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchSequences(true); // Vis loader KUN ved første innlasting
  }, [fetchSequences]);

  const onContinue = async (refId: string) => {
    try {
      await continueSequence(refId);
      fetchSequences(false /* false = ikke vis loader/spinner, behold scroll */);
      toast.success("Action accepted!");
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  const onNavigateToSequence = (referenceId: string) => {
    navigate(`/sequence/${referenceId}`);
  };

  const onDelete = async (refId: string) => {
    try {
      await fetch(`/api/sequences/${refId}/delete`, { method: "POST" });
      fetchSequences(false /* false = ikke vis loader/spinner, behold scroll */);
      toast.success("Sequence deleted!");
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  function onActionClick(refId: string, action: SequenceActions): void {
    switch (action) {
      case "Delete": {
        onDelete(refId);
        break;
      }
      case "Release": {
        onContinue(refId);
        break;
      }
    }
  }

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
            overflow: "auto", // Sørger for at det er denne boksen som scroller, ikke hele vinduet
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
              onActionClick={(action) => onActionClick(seq.referenceId, action)}
            />
          ))}
        </Box>
      )}
    </Box>
  );
}
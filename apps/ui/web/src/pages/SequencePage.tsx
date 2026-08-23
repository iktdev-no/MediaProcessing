import { Box, Typography, CircularProgress } from "@mui/material";
import { useEffect, useState, useCallback, useRef } from "react";
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

  // 1. Lag en ref for å holde på scroll-boksen
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);

  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Sequences");
  }, [setTitle]);

  const fetchSequences = useCallback((showLoader = false) => {
    // Lagre eksisterende scroll-posisjon før vi henter nytt hvis vi ikke viser loader
    const currentScrollTop = scrollContainerRef.current?.scrollTop ?? 0;

    if (showLoader) setLoading(true);

    getActiveSequences()
      .then((data) => {
        setSequences(data);

        // 2. Gjenopprett scroll-posisjonen i neste tick etter at DOM har oppdatert seg
        requestAnimationFrame(() => {
          if (scrollContainerRef.current) {
            scrollContainerRef.current.scrollTop = currentScrollTop;
          }
        });
      })
      .catch((err) => toast.error(err.message || "Kunne ikke hente sekvenser"))
      .finally(() => {
        if (showLoader) {
          setLoading(false);
        }
      });
  }, []);

  useEffect(() => {
    fetchSequences(true);
  }, [fetchSequences]);

  const onContinue = async (refId: string) => {
    try {
      await continueSequence(refId);
      fetchSequences(false);
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
      fetchSequences(false);
      toast.success("Sequence deleted!");
    } catch (err: any) {
      toast.error(err.message);
    }
  };

  function onActionClick(refId: string, action: SequenceActions): void {
    switch (action) {
      case "Delete":
        onDelete(refId);
        break;
      case "Release":
        onContinue(refId);
        break;
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
          ref={scrollContainerRef} // <-- Kobler på ref-en her
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
              onActionClick={(action) => onActionClick(seq.referenceId, action)}
            />
          ))}
        </Box>
      )}
    </Box>
  );
}
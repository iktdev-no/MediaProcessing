import { Dialog, DialogContent, DialogTitle } from "@mui/material";
import { useEffect, useState } from "react";
import { ReactFlowProvider } from "reactflow";
import { getEventsLineage } from "../../api/coordinator/events";
import type { LineageNode } from "../../types/types";
import { EventLineageGraph } from "./EventLineageGraph";

interface LineageDialogProps {
  open: boolean;
  onClose: () => void;
  referenceId: string | null;
  selectedEventId: string | null;
}

export function LineageDialog({
  open,
  onClose,
  referenceId,
  selectedEventId,
}: LineageDialogProps) {
  const [lineage, setLineage] = useState<LineageNode[]>([]);

  useEffect(() => {
    if (!open || !referenceId) return;

    getEventsLineage(referenceId).then((nodes: LineageNode[]) => {
      setLineage(nodes);
      console.log(
        `[LineageDialog] Fetched lineage for referenceId '${referenceId}': ${nodes.length} nodes.`,
      );
      console.log(nodes);
    });
  }, [open, referenceId]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Event Lineage</DialogTitle>
      <DialogContent>
        <ReactFlowProvider>
          <EventLineageGraph
            nodes={lineage}
            selectedEventId={selectedEventId}
          />
        </ReactFlowProvider>
      </DialogContent>
    </Dialog>
  );
}

import { useMemo } from "react";
import ReactFlow, {
    Background,
    Controls,
    type Edge,
    type Node,
    SmoothStepEdge
} from "reactflow";
import "reactflow/dist/style.css";

import type { LineageNode } from "../../types/transfer-model";

// Hybrid tree layout
import {
    buildHybridTree,
    computeHybridPositions
} from "./lineage/layoutHybrid";

// Must be outside component
export const edgeTypes = {
    smooth: SmoothStepEdge
};

interface Props {
    nodes: LineageNode[];
    selectedEventId: string | null;
}

export function EventLineageGraph({ nodes, selectedEventId }: Props) {
    const { rfNodes, rfEdges } = useMemo(() => {
        if (!nodes || nodes.length === 0) {
            return { rfNodes: [], rfEdges: [] };
        }

        const roots = buildHybridTree(nodes);
        const positions = computeHybridPositions(roots);

        const rfNodes: Node[] = nodes.map(n => ({
            id: n.eventId,
            type: "default",
            data: { label: n.eventName },
            position: positions.get(n.eventId) ?? { x: 0, y: 0 },
            selected: n.eventId === selectedEventId
        }));

        const rfEdges: Edge[] = [];
        nodes.forEach(n => {
            n.parents.forEach(p => {
                rfEdges.push({
                    id: `${p}-${n.eventId}`,
                    source: p,
                    target: n.eventId,
                    type: "smooth"
                });
            });
        });

        return { rfNodes, rfEdges };
    }, [nodes, selectedEventId]);

    return (
        <div style={{ width: "100%", height: 600 }}>
            <ReactFlow
                nodes={rfNodes}
                edges={rfEdges}
                edgeTypes={edgeTypes}
                fitView
            >
                <Background />
                <Controls />
            </ReactFlow>
        </div>
    );
}

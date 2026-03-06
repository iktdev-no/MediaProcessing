// components/event/lineage/LineageNodeComponent.tsx
import { Handle, Position, type NodeProps } from "reactflow";

interface LineageNodeData {
    label: string;
}

export function LineageNodeComponent({ data, selected }: NodeProps<LineageNodeData>) {
    return (
        <div
            style={{
                padding: "12px 18px",
                borderRadius: 8,
                background: selected ? "#1976d2" : "#f3f3f3",
                border: selected ? "2px solid #0d47a1" : "1px solid #ccc",
                color: selected ? "white" : "#333",
                fontWeight: selected ? "bold" : 500,
                minWidth: 160,
                textAlign: "center",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
            }}
        >
            {data.label}

            <Handle type="target" position={Position.Left} />
            <Handle type="source" position={Position.Right} />
        </div>
    );
}

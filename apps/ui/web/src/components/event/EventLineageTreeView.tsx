import type { LineageTreeNode } from "../../types/webTypes";

interface LineageTreeViewProps {
    tree: LineageTreeNode[];
    selectedEventId: string | null;
}

export function LineageTreeView({ tree, selectedEventId }: LineageTreeViewProps) {
    return (
        <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {tree.map((node, index) => (
                <PipelineNode
                    key={node.eventId}
                    node={node}
                    selectedEventId={selectedEventId}
                    isLast={index === tree.length - 1}
                    level={0}
                />
            ))}
        </div>
    );
}

interface PipelineNodeProps {
    node: LineageTreeNode;
    selectedEventId: string | null;
    level: number;
    isLast: boolean;
}

function PipelineNode({ node, selectedEventId, level, isLast }: PipelineNodeProps) {
    const isSelected = node.eventId === selectedEventId;

    return (
        <div style={{ display: "flex" }}>
            {/* Left gutter with vertical lines */}
            <div style={{ display: "flex" }}>
                {Array.from({ length: level }).map((_, i) => (
                    <div
                        key={i}
                        style={{
                            width: 20,
                            borderLeft: "2px solid #ccc",
                            marginRight: 4
                        }}
                    />
                ))}
            </div>

            {/* Node + connector */}
            <div style={{ display: "flex", flexDirection: "column" }}>
                {/* Horizontal connector */}
                {level > 0 && (
                    <div
                        style={{
                            height: 12,
                            borderTop: "2px solid #ccc",
                            marginLeft: 0
                        }}
                    />
                )}

                {/* Node bubble */}
                <div
                    style={{
                        padding: "6px 12px",
                        borderRadius: 6,
                        background: isSelected ? "#1976d2" : "#eee",
                        color: isSelected ? "white" : "black",
                        fontWeight: isSelected ? "bold" : "normal",
                        marginBottom: 4,
                        display: "inline-block"
                    }}
                >
                    {node.eventName}
                </div>

                {/* Vertical continuation line */}
                {!isLast && (
                    <div
                        style={{
                            flex: 1,
                            borderLeft: "2px solid #ccc",
                            marginLeft: 10
                        }}
                    />
                )}
            </div>

            {/* Children */}
            <div style={{ marginLeft: 20 }}>
                {node.children.map((child, index) => (
                    <PipelineNode
                        key={child.eventId}
                        node={child}
                        selectedEventId={selectedEventId}
                        level={level + 1}
                        isLast={index === node.children.length - 1}
                    />
                ))}
            </div>
        </div>
    );
}

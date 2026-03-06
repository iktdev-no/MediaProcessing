import type { LineageNode } from "../../../types/transfer-model";
import type { LineageTreeNode } from "../../../types/webTypes";

const VERTICAL_SPACING = 150;
const HORIZONTAL_SPACING = 220;
const ROOT_GROUP_OFFSET = 400;

// ------------------------------------------------------------
// Build tree (LineageNode[] → LineageTreeNode[])
// ------------------------------------------------------------
export function buildLineageTree(nodes: LineageNode[]): LineageTreeNode[] {
    const map = new Map<string, LineageTreeNode>();

    // Convert LineageNode → LineageTreeNode
    nodes.forEach(n => {
        map.set(n.eventId, { ...n, children: [] });
    });

    // Link children
    nodes.forEach(n => {
        n.parents.forEach(parentId => {
            const parent = map.get(parentId);
            const child = map.get(n.eventId);
            if (parent && child) {
                parent.children.push(child);
            }
        });
    });

    // Find roots
    let roots = [...map.values()].filter(n => n.parents.length === 0);

    // Fallback: if no roots, treat parent-less nodes as roots
    if (roots.length === 0) {
        const parentIds = new Set(nodes.flatMap(n => n.parents));

        roots = nodes
            .filter(n => !parentIds.has(n.eventId))
            .map(n => ({
                ...n,
                children: []
            }));
    }

    return roots;
}

// ------------------------------------------------------------
// Assign depth levels
// ------------------------------------------------------------
function assignLevels(
    root: LineageTreeNode,
    level = 0,
    levels = new Map<string, number>()
): Map<string, number> {
    levels.set(root.eventId, level);
    root.children.forEach(c => assignLevels(c, level + 1, levels));
    return levels;
}

// ------------------------------------------------------------
// Compute subtree width (for centering parents over children)
// ------------------------------------------------------------
function computeSubtreeWidth(node: LineageTreeNode): number {
    if (node.children.length === 0) return 1;
    return node.children.map(computeSubtreeWidth).reduce((a, b) => a + b, 0);
}

// ------------------------------------------------------------
// Compute positions with parent-centering
// ------------------------------------------------------------
export function computeLineagePositions(
    roots: LineageTreeNode[]
): Map<string, { x: number; y: number }> {
    const positions = new Map<string, { x: number; y: number }>();

    roots.forEach((root, rootIndex) => {
        const levels = assignLevels(root);

        function placeNode(node: LineageTreeNode, xOffset: number) {
            const level = levels.get(node.eventId)!;
            const subtreeWidth = computeSubtreeWidth(node);

            // Center node horizontally within its subtree
            const x = xOffset + (subtreeWidth * HORIZONTAL_SPACING) / 2;

            positions.set(node.eventId, {
                x: x + rootIndex * ROOT_GROUP_OFFSET,
                y: level * VERTICAL_SPACING
            });

            // Place children
            let childOffset = xOffset;
            node.children.forEach(child => {
                const childWidth = computeSubtreeWidth(child);
                placeNode(child, childOffset);
                childOffset += childWidth * HORIZONTAL_SPACING;
            });
        }

        placeNode(root, 0);
    });

    return positions;
}

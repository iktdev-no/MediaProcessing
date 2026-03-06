import type { LineageNode } from "../../../types/transfer-model";
import type { LineageTreeNode } from "../../../types/webTypes";

const VERTICAL_SPACING = 150;
const HORIZONTAL_SPACING = 220;

/**
 * Build a DAG-aware tree structure without duplicating nodes.
 * Includes detailed logging for missing parents.
 */
export function buildHybridTree(nodes: LineageNode[]): LineageTreeNode[] {
    const map = new Map<string, LineageTreeNode>();

    // 1. Lag map
    nodes.forEach(n => {
        map.set(n.eventId, { ...n, children: [] });
    });

    // 2. Koble parent → child
    nodes.forEach(n => {
        n.parents.forEach(parentId => {
            const parent = map.get(parentId);
            const child = map.get(n.eventId);

            if (parent && child) {
                parent.children.push(child);
            }
        });
    });

    // 3. Roots = nodes with no parents
    const roots = [...map.values()].filter(n => n.parents.length === 0);

    return roots;
}


/**
 * Compute subtree width for centering.
 */
function computeSubtreeWidth(node: LineageTreeNode): number {
    if (node.children.length === 0) return 1;
    return node.children
        .map(computeSubtreeWidth)
        .reduce((a, b) => a + b, 0);
}

/**
 * Compute positions with parent-centering.
 */
export function computeHybridPositions(
    roots: LineageTreeNode[]
): Map<string, { x: number; y: number }> {
    const positions = new Map<string, { x: number; y: number }>();

    function place(node: LineageTreeNode, xOffset: number, level: number) {
        const width = computeSubtreeWidth(node);
        const x = xOffset + (width * HORIZONTAL_SPACING) / 2;

        positions.set(node.eventId, {
            x,
            y: level * VERTICAL_SPACING
        });

        let childOffset = xOffset;
        node.children.forEach(child => {
            const w = computeSubtreeWidth(child);
            place(child, childOffset, level + 1);
            childOffset += w * HORIZONTAL_SPACING;
        });
    }

    let offset = 0;
    roots.forEach(root => {
        const w = computeSubtreeWidth(root);
        place(root, offset, 0);
        offset += w * HORIZONTAL_SPACING * 1.5;
    });

    return positions;
}

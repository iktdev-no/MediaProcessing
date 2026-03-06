// types/dagre.d.ts
declare module "dagre" {
    export namespace graphlib {
        class Graph {
            constructor(options?: any);
            setGraph(options: any): void;
            setDefaultEdgeLabel(callback: () => any): void;
            setNode(id: string, value: { width: number; height: number }): void;
            setEdge(source: string, target: string, value?: any): void;
            node(id: string): { x: number; y: number; width: number; height: number };
        }
    }

    export function layout(graph: graphlib.Graph): void;
}

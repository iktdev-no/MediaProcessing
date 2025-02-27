

export interface TablePropetyConfig {
    label: string
    accessor: string
}

export interface TableCellCustomizer<T> {
    (accessor: string, data: T): JSX.Element | null
}

type NullableTableRowActionEvents<T> = TableRowActionEvents<T> | null;
export interface TableRowActionEvents<T> {
    click: (row: T) => void;
    doubleClick: (row: T) => void;
    contextMenu?: (row: T, x: number, y: number) => void;
}
import { Box, TableContainer } from "@mui/material";

export interface ITableRow<T> {
    rowId: string;
    title: string;
}

export interface TableRowItem<T> extends ITableRow<T> {
    rowId: string;
    title: string;
    item: T;
}

export interface TableRowGroupedItem<T> extends ITableRow<T> {
    rowId: string;
    title: string;
    items: Array<T>
}


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

export type SortBy = 'asc' | 'desc';

export interface SortByAccessor {
    accessor: string
    order: SortBy
}

export type TableSorter<T> = (a: T, b: T, orderBy: SortBy, accessor: string) => number;


export type SortableGroupedTableProps<R extends TableRowGroupedItem<U>, U> = {
    items: Array<R>;
    columns: Array<TablePropetyConfig>;
    cellCustomizer?: TableCellCustomizer<R>;
    onRowClickedEvent?: NullableTableRowActionEvents<R>;
    defaultSort?: SortByAccessor;
}


export function DefaultTableContainer({children}: {children?: JSX.Element}) {
    return (
        <Box sx={{
            display: "flex",
            flexDirection: "column", // Bruk column-fleksretning
            height: "100%",
            overflow: "hidden"
        }}>
            <TableContainer sx={{
                flex: 1,
                overflowY: "auto",
                position: "relative", // Legg til denne linjen for å justere layout
                maxHeight: "100%" // Legg til denne linjen for å begrense høyden
            }}>
                {children}
            </TableContainer>
        </Box >
    );
}


export function KeybasedComparator(a: any, b: any, key: string) {
    if (typeof a[key] === 'string') {
        return a[key].localeCompare(b[key]);
    } else if (typeof a[key] === 'number') {
        return a[key] - b[key];
    }
    return 0;
}

export function SortGroupedTableItemsOnRoot<R extends TableRowItem<U>, U>(
    sortBy: SortByAccessor,
    items: Array<R>,
) {
    return items.slice().sort((a, b) => {
        if (sortBy.order === 'asc') {
            return KeybasedComparator(a, b, sortBy.accessor);
        }
        return KeybasedComparator(b, a, sortBy.accessor);
    });
}

export function SortGroupedTableItemsOnChilds<R extends TableRowGroupedItem<U>, U>(
    sortBy: SortByAccessor,
    items: Array<R>
): Array<R> {
    return items.map(item => ({
        ...item,
        items: item.items?.slice().sort((a, b) => {
            if (sortBy.order === "asc") {
                return KeybasedComparator(a, b, sortBy.accessor);
            }
            return KeybasedComparator(b, a, sortBy.accessor);
        })
    })).sort((a, b) => {
        if (sortBy.order === "asc") {
            return KeybasedComparator(a, b, sortBy.accessor);
        }
        return KeybasedComparator(b, a, sortBy.accessor);
    });
}


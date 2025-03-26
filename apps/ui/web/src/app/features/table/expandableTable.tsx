import { Box, TableContainer, Table, TableHead, TableRow, TableCell, Typography, TableBody, useTheme } from "@mui/material";
import { useState, useEffect, useMemo } from "react";
import { TablePropetyConfig, TableCellCustomizer, TableRowActionEvents } from "./table";
import IconArrowUp from '@mui/icons-material/ArrowUpward';
import IconArrowDown from '@mui/icons-material/ArrowDownward';

export interface ExpandableItem<T> {
    tag: string;
    expandElement: JSX.Element | null;
}

export type ExpandableRender<T> = (item: T) => ExpandableItem<T> | null;
export interface ExpandableTableItem {
    rowId: string
}

export interface SortByAccessor {
    accessor: string
    order: 'asc' | 'desc'
}

export default function ExpandableTable<T extends ExpandableTableItem>({ items, columns, cellCustomizer: customizer, expandableRender, onRowClickedEvent, defaultSort}: { items: Array<T>, columns: Array<TablePropetyConfig>, cellCustomizer?: TableCellCustomizer<T>, expandableRender: ExpandableRender<T>,  onRowClickedEvent?: TableRowActionEvents<T>, defaultSort?: SortByAccessor }) {
    const muiTheme = useTheme();
    
    const [order, setOrder] = useState<'asc' | 'desc'>('asc');
    const [orderBy, setOrderBy] = useState<string>('');
    const [expandedRowIds, setExpandedRowIds] = useState<Set<string>>(new Set());
    const [selectedRow, setSelectedRow] = useState<T | null>(null);
    const [selectedRowId, setSelectedRowId] = useState<string | null>(null);

    const tableRowSingleClicked = (row: T | null) => {
        if (row != null && 'rowId' in row) {
            setExpandedRowIds(prev => {
                const newExpandedRows = new Set(prev);
                if (newExpandedRows.has(row.rowId)) {
                    newExpandedRows.delete(row.rowId);
                } else {
                    newExpandedRows.add(row.rowId);
                }
                return newExpandedRows;
            })
        }

        if (row === selectedRow) {
            setSelectedRow(null);
            setSelectedRowId(null);
        } else {
            setSelectedRow(row);
            setSelectedRowId(row?.rowId ?? null);
            if (row && onRowClickedEvent) {
                onRowClickedEvent.click(row);
            }
        }

    }
    const tableRowDoubleClicked = (row: T | null) => {
        setSelectedRow(row);
        if (row && onRowClickedEvent) {
            onRowClickedEvent.doubleClick(row);
        }
    }

    const tableRowContextMenu = (e: React.MouseEvent<HTMLTableRowElement, MouseEvent> , row: T | null) => {
        if (row && onRowClickedEvent && onRowClickedEvent.contextMenu) {
            e.preventDefault()
            onRowClickedEvent.contextMenu(row, e.pageX, e.pageY)
        }
    }

    const handleSort = (property: string) => {
        const isAsc = orderBy === property && order === 'asc';
        setOrder(isAsc ? 'desc' : 'asc');
        setOrderBy(property);
    };

    const compareValues = (a: any, b: any, orderBy: string) => {
        if (typeof a[orderBy] === 'string') {
            return a[orderBy].localeCompare(b[orderBy]);
        } else if (typeof a[orderBy] === 'number') {
            return a[orderBy] - b[orderBy];
        }
        return 0;
    };


    const sortedData = useMemo(() => {
        return items.slice().sort((a, b) => {
            if (order === 'asc') {
                return compareValues(a, b, orderBy);
            } else {
                return compareValues(b, a, orderBy);
            }
        });
    }, [items, order, orderBy]);    

    useEffect(() => {
        handleSort(columns[0].accessor)
        if (defaultSort) {
            setOrder(defaultSort.order);
            setOrderBy(defaultSort.accessor);
        }
    }, [defaultSort])

    useEffect(() => {
        if (selectedRowId) {
            const matchingRow = items.find((item) => item.rowId === selectedRowId);
            if (matchingRow) {
                setSelectedRow(matchingRow);
            } else {
                setSelectedRow(null); // Hvis raden ikke finnes lenger, fjern valg
                setSelectedRowId(null);
            }
        }
    }, [items, selectedRowId]);

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
                <Table>
                    <TableHead sx={{
                        position: "sticky",
                        top: 0,
                        backgroundColor: muiTheme.palette.background.paper,
                    }}>
                        <TableRow key={`orderRow-${Math.random()}`}>
                            {columns.map((column) => (
                                <TableCell key={column.accessor} onClick={() => handleSort(column.accessor)} sx={{ cursor: "pointer" }}>
                                    <Box display="flex">
                                        {orderBy === column.accessor ?
                                            (order === "asc" ? (<IconArrowDown />) : (<IconArrowUp />)) : (
                                                <IconArrowDown sx={{ color: "transparent" }} />
                                            )
                                        }
                                        <Typography>{column.label}</Typography>
                                    </Box>
                                </TableCell>
                            ))}
                        </TableRow>
                    </TableHead>
                    <TableBody sx={{
                        overflowY: "scroll"
                    }}>
                        {sortedData?.map((row: T, rowIndex: number) => [
                            <TableRow key={row.rowId}
                                onClick={() => tableRowSingleClicked(row)}
                                onDoubleClick={() => tableRowDoubleClicked(row)}
                                onContextMenu={(e) => {
                                    tableRowContextMenu(e, row);
                                    tableRowSingleClicked(row);
                                }}
                                style={{ cursor: "pointer", backgroundColor: selectedRow === row ? muiTheme.palette.action.selected : '' }}
                            >
                                {columns.map((column) => (
                                    <TableCell key={column.accessor}>
                                        {customizer && customizer(column.accessor, row) !== null
                                            ? customizer(column.accessor, row)
                                            : <Typography variant='body1'>{(row as any)[column.accessor]}</Typography>}
                                    </TableCell>
                                ))}
                            </TableRow>,
                        (expandedRowIds.has(row.rowId)) ?
                            (<TableRow key={row.rowId + "-expanded"}>
                                <TableCell colSpan={columns.length}>
                                    {
                                        expandableRender(row)?.expandElement
                                    }
                                </TableCell>
                            </TableRow>): null
                        
                        ])}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    )
}
import { Box, TableContainer, Table, TableHead, TableRow, TableCell, Typography, TableBody, useTheme } from "@mui/material";
import { useState, useEffect } from "react";
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

export default function ExpandableTable<T extends ExpandableTableItem>({ items, columns, cellCustomizer: customizer, expandableRender, onRowClickedEvent }: { items: Array<T>, columns: Array<TablePropetyConfig>, cellCustomizer?: TableCellCustomizer<T>, expandableRender: ExpandableRender<T>,  onRowClickedEvent?: TableRowActionEvents<T> }) {
    const muiTheme = useTheme();
    
    const [order, setOrder] = useState<'asc' | 'desc'>('asc');
    const [orderBy, setOrderBy] = useState<string>('');
    const [expandedRowIds, setExpandedRowIds] = useState<Set<string>>(new Set());
    const [selectedRow, setSelectedRow] = useState<T | null>(null);

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
        } else {
            setSelectedRow(row);
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

    const sortedData = items.slice().sort((a, b) => {
        if (order === 'asc') {
            return compareValues(a, b, orderBy);
        } else {
            return compareValues(b, a, orderBy);
        }
    });

    useEffect(() => {
        handleSort(columns[0].accessor)
    }, [])


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
                        <TableRow>
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
                        {sortedData?.map((row: T, rowIndex: number) => (
                            <>
                                <TableRow key={rowIndex}
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
                                </TableRow>
                                {(expandedRowIds.has(row.rowId)) ?
                                    (<TableRow key={rowIndex + "_1"}>
                                        <TableCell colSpan={columns.length}>
                                            {
                                                expandableRender(row)?.expandElement
                                            }
                                        </TableCell>
                                    </TableRow>): null
                                }
                                
                            </>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    )
}
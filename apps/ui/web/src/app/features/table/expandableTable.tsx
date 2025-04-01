import { Box, TableContainer, Table, TableHead, TableRow, TableCell, Typography, TableBody, useTheme } from "@mui/material";
import { useState, useEffect, useMemo } from "react";
import { TablePropetyConfig, TableCellCustomizer, TableRowActionEvents, SortByAccessor, TableRowItem, DefaultTableContainer, ITableRow, SortBy, TableSorter } from "./table";
import IconArrowUp from '@mui/icons-material/ArrowUpward';
import IconArrowDown from '@mui/icons-material/ArrowDownward';


export type ExpandableRender<T> = (item: T) => JSX.Element | null;


export default function ExpandableTable<R extends ITableRow<U>, U>({ items, columns, cellCustomizer: customizer, expandableRender, onRowClickedEvent, defaultSort, sorter }: { items: Array<R>, columns: Array<TablePropetyConfig>, cellCustomizer?: TableCellCustomizer<R>, expandableRender: ExpandableRender<R>,  onRowClickedEvent?: TableRowActionEvents<R>, defaultSort?: SortByAccessor, sorter?: TableSorter<R> }) {
    const muiTheme = useTheme();
    
    const [order, setOrder] = useState<'asc' | 'desc'>(defaultSort?.order ?? 'asc');
    const [orderBy, setOrderBy] = useState<string>(defaultSort?.accessor ?? columns[0].accessor ?? '');
    const [expandedRowIds, setExpandedRowIds] = useState<Set<string>>(new Set());
    const [selectedRow, setSelectedRow] = useState<R | null>(null);
    const [selectedRowId, setSelectedRowId] = useState<string | null>(null);

    const tableRowSingleClicked = (row: R | null) => {
        console.log("tableRowSingleClicked", row)
        if (row != null && 'rowId' in row) {
            setExpandedRowIds(prev => {
                const newExpandedRows = new Set(prev);
                console.log("newExpandedRows", newExpandedRows)
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
    const tableRowDoubleClicked = (row: R | null) => {
        setSelectedRow(row);
        if (row && onRowClickedEvent) {
            onRowClickedEvent.doubleClick(row);
        }
    }

    const tableRowContextMenu = (e: React.MouseEvent<HTMLTableRowElement, MouseEvent> , row: R | null) => {
        if (row && onRowClickedEvent && onRowClickedEvent.contextMenu) {
            e.preventDefault()
            onRowClickedEvent.contextMenu(row, e.pageX, e.pageY)
        }
    }


    const compareValues = (a: any, b: any, orderBy: string) => {
        console.log("compareValues", a, b, orderBy)
        if (typeof a[orderBy] === 'string') {
            return a[orderBy].localeCompare(b[orderBy]);
        } else if (typeof a[orderBy] === 'number') {
            return a[orderBy] - b[orderBy];
        }
        return 0;
    };


    const sortedData = useMemo(() => {
        return [...items].sort((a, b) => {
            if (sorter) {
                return sorter(a, b, order, orderBy);
            } else {
                if (order === 'asc') {
                    return compareValues(a, b, orderBy);
                } else {
                    return compareValues(b, a, orderBy);
                }
            }
        }
        );
    }, [items, order, orderBy]);    

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


    const handleSort = (property: string) => {
        const isAsc = orderBy === property && order === 'asc';
        setOrder(isAsc ? 'desc' : 'asc');
        setOrderBy(property);
        console.log("handleSort", property, isAsc ? 'desc' : 'asc')
    };

    return (
        <DefaultTableContainer>
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
                        {sortedData?.map((row: R, rowIndex: number) => [
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
                                        expandableRender(row)
                                    }
                                </TableCell>
                            </TableRow>): null
                        
                        ])}
                    </TableBody>
                </Table>
        </DefaultTableContainer>
    )
}
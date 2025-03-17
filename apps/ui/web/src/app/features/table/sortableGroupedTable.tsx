import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { RootState } from "../../store";
import IconArrowUp from '@mui/icons-material/ArrowUpward';
import IconArrowDown from '@mui/icons-material/ArrowDownward';
import { Table, TableHead, TableRow, TableCell, TableBody, Typography, Box, useTheme, TableContainer, IconButton } from "@mui/material";
import { TablePropetyConfig, TableCellCustomizer, TableRowActionEvents } from "./table";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

export interface TableItemGroup<T> {
    title: string
    items: Array<T>
}

export default function SortableGroupedTable<T>({ items, columns, customizer, onRowClickedEvent }: { items: Array<TableItemGroup<T>>, columns: Array<TablePropetyConfig>, customizer?: TableCellCustomizer<T>, onRowClickedEvent?: TableRowActionEvents<T> }) {
    const muiTheme = useTheme();
    
    const [order, setOrder] = useState<'asc' | 'desc'>('asc');
    const [orderBy, setOrderBy] = useState<string>('');
    const [selectedRow, setSelectedRow] = useState<T | null>(null);
    const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());
    const toggleExpand = (index: number) => {
        setExpandedRows((prev) => {
            const newSet = new Set(prev);
            if (newSet.has(index)) {
                newSet.delete(index); // Collapse if already expanded
            } else {
                newSet.add(index); // Expand if not already expanded
            }
            return newSet;
        });
    };

    const tableRowSingleClicked = (row: T | null) => {
        setSelectedRow(row);
        if (row && onRowClickedEvent) {
            onRowClickedEvent.click(row);
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

    const sortedData: Array<TableItemGroup<T>> = items.map((item: TableItemGroup<T>) => {
        return {
            title: item.title,
            items: item.items.slice().sort((a, b) => {
                if (order === 'asc') {
                    return compareValues(a, b, orderBy);
                } else {
                    return compareValues(b, a, orderBy);
                }
            })  
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
                    
                    {sortedData.map((item, index) => (
                        <>
                            <TableHead 
                            sx={{
                                backgroundColor: muiTheme.palette.primary.dark,
                            }}>
                                <TableRow>
                                    <TableCell colSpan={columns.length}>
                                        <Box onClick={() => toggleExpand(index)} sx={{ display: "flex", justifyContent: "space-between" }}>
                                            <Typography variant='h6'>{item.title}</Typography>
                                            <IconButton onClick={() => toggleExpand(index)}>
                                                {expandedRows.has(index) ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                                            </IconButton>
                                        </Box>
                                    </TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody sx={{
                                display: expandedRows.has(index) ? 'table-row-group' : 'none',
                                overflowY: "scroll"
                            }}>
                                {item.items?.map((row: T, rowIndex: number) => (
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
                                ))}
                            </TableBody>
                        </>
                    ))}

                </Table>
            </TableContainer>
        </Box>
    )
}
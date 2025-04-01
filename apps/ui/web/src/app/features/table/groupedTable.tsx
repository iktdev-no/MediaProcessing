import { useEffect, useState } from "react";

import { ExpandableRender } from "./expandableTable";
import { DefaultTableContainer, SortByAccessor, TableCellCustomizer, TablePropetyConfig, TableRowActionEvents, TableRowGroupedItem } from "./table";
import { useTheme } from "@mui/material";

export interface GroupedTableProps<R extends TableRowGroupedItem<U>, U> {
    items: Array<R>;   
    columns: Array<TablePropetyConfig>;
    cellCustomizer?: TableCellCustomizer<R>;
    onRowClickedEvent?: TableRowActionEvents<R>;
    defaultSort?: SortByAccessor;

}


export default function GroupedTable<R extends TableRowGroupedItem<U>, U>({ items, columns, cellCustomizer: customizer, onRowClickedEvent }: GroupedTableProps<R, U>) {
    const muiTheme = useTheme();

    const [order, setOrder] = useState<'asc' | 'desc'>('asc');
    const [orderBy, setOrderBy] = useState<string>('');
    const [selectedRowId, setSelectedRowId] = useState<string | null>(null);
 
    const handleSort = (property: string) => {
        const isAsc = orderBy === property && order === 'asc';
        setOrder(isAsc ? 'desc' : 'asc');
        setOrderBy(property);
    };

    return (
        <></>
    );
}
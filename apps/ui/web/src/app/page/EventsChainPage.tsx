import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useStompClient } from 'react-stomp-hooks';
import { RootState } from "../store";
import { useWsSubscription } from "../ws/subscriptions";
import { EventsObjectListResponse } from "../../types";
import IconRefresh from '@mui/icons-material/Refresh'
import { Box, Button, Typography, useTheme } from "@mui/material";
import { Tree } from "react-d3-tree";
import { EventChain, EventGroup, EventGroups, set } from "../store/chained-events-slice";
import { toUnixTimestamp, UnixTimestamp } from "../features/UxTc";
import { TableCellCustomizer, TablePropetyConfig } from "../features/table/table";
import ExpandableTable, { ExpandableItem } from "../features/table/expandableTable";
import { CustomNodeElementProps } from 'react-d3-tree';


interface RawNodeDatum {
    name: string;
    attributes?: Record<string, string | number | boolean>;
    children?: RawNodeDatum[];
    fill?: string;
}

// Transformasjonsfunksjon for EventChain
const transformEventChain = (eventChain: EventChain): RawNodeDatum => ({
    name: eventChain.eventName,
    attributes: {
        //eventId: eventChain.eventId,
        created: toUnixTimestamp({ timestamp: eventChain.created }) ?? "",
        success: eventChain.success,
        skipped: eventChain.skipped,
        failure: eventChain.failure,
        childrens: eventChain.events.length
    },
    fill: "white",
    children: eventChain.events.map(transformEventChain),
});

// Transformasjonsfunksjon for EventGroups
const transformEventGroups = (group: EventGroup): RawNodeDatum[] => {
    return group.events.map(transformEventChain);
}

interface EventGroupToTreeView extends EventGroup {
    underView: JSX.Element
}

export default function EventsChainPage() {
    const muiTheme = useTheme();
    const dispatch = useDispatch();
    const client = useStompClient();
    const cursor = useSelector((state: RootState) => state.chained)
    const [useReferenceId, setUseReferenceId] = useState<string | null>(null);
    const [treeData, setTreeData] = useState<RawNodeDatum[] | null>(null);

    useWsSubscription<Array<EventGroup>>("/topic/chained/all", (response) => {
        dispatch(set(response))
    });


    useEffect(() => {
        client?.publish({
            destination: "/app/chained/all"
        });
    }, [client, dispatch]);

    const onRefresh = () => {
        client?.publish({
            "destination": "/app/chained/all",
            "body": "Potato"
        })
    }

    useEffect(() => {
        if (useReferenceId) {
            const eventGroup = cursor.groups.find((group) => group.referenceId === useReferenceId);
            if (eventGroup) {
                const data = transformEventGroups(eventGroup);
                console.log({
                    "info": "Tree data",
                    "data": data
                })
                setTreeData(data);
            }
        }
    }, [useReferenceId, cursor])

    const createTableCell: TableCellCustomizer<EventGroup> = (accessor, data) => {
        switch (accessor) {
            case "created": {
                if (typeof data[accessor] === "number") {
                    const timestampObject = { timestamp: data[accessor] as number }; // Opprett et objekt med riktig struktur
                    return UnixTimestamp(timestampObject);
                } else {
                    return null;
                }
            }
            default: return null;
        }
    };

    const columns: Array<TablePropetyConfig> = [
        { label: "ReferenceId", accessor: "referenceId" },
        { label: "File", accessor: "fileName" },
        { label: "Created", accessor: "created" },
    ];

    
    function renderCustomNodeElement(nodeData: CustomNodeElementProps): JSX.Element {
        const attr = nodeData.nodeDatum.attributes; 
        const nodeColor = attr?.success ? "green" : attr?.failure ? "red" : "yellow";
        return (<>
          <g>
                <circle r="15" fill={nodeColor} onClick={nodeData.toggleNode} />
                <text fill="white" stroke="white" strokeWidth="0" x="20">
                    {nodeData.nodeDatum.name}
                </text>
                {attr?.created && (
                <text fill="lightgray" x="20" dy="20" strokeWidth="0">
                    Created: {attr?.created}
                </text>
                )}
                {attr?.success && (
                    <text fill="lightgray" x="20" dy="40" strokeWidth="0">
                        Success
                    </text>
                )}
                {attr?.failure && (
                    <text fill="lightgray" x="20" dy="40" strokeWidth="0">
                        Failure
                    </text>
                )}
                {attr?.skipped && (
                    <text fill="lightgray" x="20" dy="40" strokeWidth="0">
                        Skipped
                    </text>
                )}
                {attr?.childrens && (
                    <text fill="lightgray" x="20" dy="60" strokeWidth="0">
                        {attr?.childrens} derived {Number(attr?.childrens) > 1 ? "events" : "event"}
                    </text>
                )}
            </g>
        </>);
    }

    function renderExpandableItem(item: EventGroup): ExpandableItem<EventGroup> | null {
        return {
            tag: item.referenceId,
            expandElement: (() => {
                const data = transformEventGroups(item);
                return (
                    <>
                        {(data) ? (
                            <div id="treeWrapper" style={{ width: '100%', height: '60vh',  }}>
                                <Tree data={data} orientation="vertical" separation={{
                                    nonSiblings: 3,
                                    siblings: 2
                                }} 
                                renderCustomNodeElement={renderCustomNodeElement}
                                 />
                            </div>
                        ) : <Typography>Tree data not available</Typography>}
                    </>
                );
            })()
        };
    }


    return (
        <>
            <Button
                startIcon={<IconRefresh />}
                onClick={onRefresh} sx={{
                    borderRadius: 5,
                    textTransform: 'none'
                }}>Refresh
            </Button>
            <Box display="block">
                <Box sx={{
                    display: "block",
                    height: "calc(100% - 120px)",
                    overflow: "hidden",
                    position: "absolute",
                    width: "100%"
                }}>
                    <ExpandableTable items={cursor?.groups ?? []} columns={columns} cellCustomizer={createTableCell} expandableRender={renderExpandableItem} />
                </Box>
            </Box>
        </>
    )
}
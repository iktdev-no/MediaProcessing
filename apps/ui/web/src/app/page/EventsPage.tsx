import Tree, { CustomNodeElementProps } from "react-d3-tree";
import { useWsSubscription } from "../ws/subscriptions"
import NotStartedIcon from '@mui/icons-material/NotStarted';
import ReactDOMServer from 'react-dom/server';
import React, { ReactNode, useCallback, useEffect, useState } from "react";
import { extractSvgContent, useCenteredTree } from "../features/util";
import './EventsPage.css';
import ClearIcon from '@mui/icons-material/Clear';
import KeyboardDoubleArrowRightIcon from '@mui/icons-material/KeyboardDoubleArrowRight';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import SubtitlesIcon from '@mui/icons-material/Subtitles';
import MovieIcon from '@mui/icons-material/Movie';
import CheckIcon from '@mui/icons-material/Check';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import AutoAwesomeMotionIcon from '@mui/icons-material/AutoAwesomeMotion';
import { client } from "stompjs";
import { useStompClient } from "react-stomp-hooks";
import { useDispatch, useSelector } from "react-redux";
import { ContentEventState, ContentEventStateItems, ProcesserEventInfo, Status, update, updateEncodeProgress, WorkStatus } from "../store/work-slice";
import ExpandableTable from "../features/table/expandableTable";
import { RootState } from "../store";
import { KeybasedComparator, SortBy, TableCellCustomizer, TablePropetyConfig, TableRowItem } from "../features/table/table";
import SimpleTable from "../features/table/sortableTable";
import { UnixTimestamp } from "../features/UxTc";
import ProgressbarWithLabel from "../features/components/ProgressbarWithLabel";
import { LinearProgress, Typography } from "@mui/material";
import { stat } from "fs";

interface RawNodeDatum {
    name: string;
    attributes?: Record<string, string | number | boolean>;
    children?: RawNodeDatum[];
    fill?: string;
}

interface EventNodeData {
    statusIcon: JSX.Element | null
    statusColor: string
}

function getEventNodeStatus(status: Status): EventNodeData {
    const toData = (status: Status): EventNodeData => {
        switch (status) {
            case Status.NeedsApproval: {
                return {
                    statusIcon: <NotStartedIcon />,
                    statusColor: "crimson"
                } as EventNodeData
            }
            case Status.Completed: {
                return {
                    statusIcon: <CheckIcon />,
                    statusColor: "forestgreen"
                }
            }
            case Status.Skipped: {
                return {
                    statusIcon: <KeyboardDoubleArrowRightIcon />,
                    statusColor: "#313131"
                }
            }
            case Status.Awaiting: {
                return {
                    statusIcon: <MoreHorizIcon />,
                    statusColor: "#313131"
                }
            }
            case Status.Pending: {
                return {
                    statusIcon: <HourglassEmptyIcon />,
                    statusColor: "#ffa000"
                }
            }
            case Status.InProgress: {
                return {
                    statusIcon: <HourglassEmptyIcon />,
                    statusColor: "dodgerblue"
                }
            }
            case Status.Failed: {
                return {
                    statusIcon: <ClearIcon />,
                    statusColor: "crimson"
                }
            }
        }
    }

    return toData(status);
}


function renderCustomNodeElement(nodeData: CustomNodeElementProps): JSX.Element {
    const attr = nodeData.nodeDatum.attributes;
    let workIcon: JSX.Element | null = null
    switch (attr?.type) {
        case "encode": {
            workIcon = <MovieIcon />
            break;
        }
        case "extract": {
            workIcon = <SubtitlesIcon />
            break;
        }
        case "convert": {
            workIcon = <AutoAwesomeMotionIcon />
            break;
        }
    }



    const status = getEventNodeStatus((attr?.status as Status) ?? Status.Awaiting)

    return (<>
        <g>
            <circle r="20" strokeWidth={3} fill={status.statusColor} />
            <g transform="scale(1.5)" stroke="none" fill="white">
                {extractSvgContent(status.statusIcon)}
            </g>
            <g transform="translate(0, 45) scale(1)" stroke="none" fill="white">
                {extractSvgContent(workIcon)}
            </g>
        </g>


    </>);
}

const taskOperationStatusToStatus = (status: WorkStatus): Status | undefined => {
    switch (status) {
        case WorkStatus.Completed:
            return Status.Completed;
        case WorkStatus.Failed:
            return Status.Failed;
        case WorkStatus.Pending:
            return Status.Pending;
        case WorkStatus.Started:
        case WorkStatus.Working:
            return Status.InProgress;
    }
}


const transformToSteps = (state: ContentEventState): RawNodeDatum => ({
    name: state.referenceId,
    attributes: {
        type: "encode",
        status: taskOperationStatusToStatus(state?.encodeWork?.status) ?? state.encode
    },
    children: [
        {
            name: state.referenceId,
            attributes: {
                type: "extract",
                status: state.extract
            },
            children: [
                {
                    name: state.referenceId,
                    attributes: {
                        type: "convert",
                        status: state.extract
                    },
                }
            ]
        },
    ]
});

export type ExpandableItemRow = TableRowItem<ContentEventState>

export default function EventsPage() {
    const client = useStompClient();
    const dispatch = useDispatch();
    const events: ContentEventStateItems = useSelector((state: RootState) => state.work);
    const [tableItems, setTableItems] = useState<Array<ExpandableItemRow>>([]);

    useEffect(() => {  
        const items = events.items.map((event: ContentEventState) => {
            return {
                rowId: event.referenceId,
                title: event.referenceId,
                item: event
            } as ExpandableItemRow
        });
        setTableItems(items);
    }, [events]);

    useWsSubscription<Array<ContentEventState>>("/topic/tasks/all", (response) => {
        console.log(response)
        dispatch(update(response))
    });

    useWsSubscription<ProcesserEventInfo>("/topic/processer/encode/progress", (response) => {
        console.log(response)
        dispatch(updateEncodeProgress(response))
    })

    useEffect(() => {
        client?.publish({
            destination: "/app/tasks/all"
        })
    }, [client]);

    const createCellTable: TableCellCustomizer<ExpandableItemRow> = (accessor, data) => {
        switch (accessor) {
            case "runners": {
                return (<>
                    <div id="treeWrapper" style={{
                        ...linkThicc,
                        width: '150px', height: '90px'
                    }} ref={containerRef}>
                        <Tree
                            dimensions={dimensions}
                            translate={{
                                x: 24,
                                y: 24
                            }}
                            data={transformToSteps(data.item)}
                            orientation="horizontal"
                            separation={{
                                nonSiblings: 1,
                                siblings: 1,
                            }}
                            nodeSize={{
                                x: 50,
                                y: 50
                            }}
                            draggable={false}
                            zoomable={false}
                            renderCustomNodeElement={renderCustomNodeElement}
                            pathClassFunc={() => 'thicc-link'}

                        />
                    </div>
                </>)
            };
            case "created": {
                if (typeof data.item[accessor] === "number") {
                    return UnixTimestamp({ timestamp: data.item[accessor] });
                }
                return null;
            }
            default: return null;
        }
    };

    const sorter = (a: ExpandableItemRow, b: ExpandableItemRow, orderBy: SortBy, accessor: string) => { 
        if (orderBy === "asc") {
            return KeybasedComparator(a.item, b.item, "created")
        } else {
            return KeybasedComparator(b.item, a.item, "created")
        }
    }

    function renderExpandableItem(item: ExpandableItemRow): JSX.Element | null {
        const data = item.item;
        const progress = data.encodeWork?.progress?.progress ?? undefined;
        const showProgressbar = [WorkStatus.Pending, WorkStatus.Started, WorkStatus.Working, WorkStatus.Completed].includes(data?.encodeWork?.status)

        const processer = data.encodeWork // events.encodeWork[item.referenceId];
        const showIndeterminate = processer?.status in [WorkStatus.Pending, WorkStatus.Started] || processer?.progress?.progress <= 0
        console.log({
            type: "info",
            processer: processer,
            showIndeterminate: showIndeterminate,
            showProgressbar: showProgressbar,
            isWorking: data.encodeWork?.status == WorkStatus.Working
        });
        return (
            <>
                <Typography>{data.encodeWork?.progress?.timeLeft}</Typography>
                {(showProgressbar) ?
                    <ProgressbarWithLabel indeterminateText={"Waiting"} progress={progress} /> : null
                }
            </>
        );
    }

    const columns: Array<TablePropetyConfig> = [
        { label: "Title", accessor: "title" },
        { label: "Started", accessor: "created" },
        { label: "", accessor: "runners" },
    ];


    const [dimensions, translate, containerRef] = useCenteredTree();

    const linkThicc = {
        strokeWith: 5
    }

    return (
        <>
            <ExpandableTable items={tableItems} columns={columns} cellCustomizer={createCellTable} expandableRender={renderExpandableItem} defaultSort={{
                order: 'desc',
                accessor: "created"
            }} sorter={sorter} />
        </>
    )
}
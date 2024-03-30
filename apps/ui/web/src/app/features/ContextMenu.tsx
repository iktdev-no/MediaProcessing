import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store";
import { setContextMenuVisible } from "../store/context-menu-slice";
import { Box, Typography, useTheme } from "@mui/material";

export interface ContextMenuItem {
  actionIndex: number,
  icon: JSX.Element | null,
  text: string
}

type NullableContextMenuActionEvent<T> = ContextMenuActionEvent<T> | null;
export interface ContextMenuActionEvent<T> {
  selected: (actionIndex: number | null, value: T | null) => void;
}

export default function ContextMenu<T>({ actionItems, row = null, onContextMenuItemClicked} : { actionItems: ContextMenuItem[], row?: T | null, onContextMenuItemClicked?: ContextMenuActionEvent<T>}) {
  const muiTheme = useTheme();
  const state = useSelector((state: RootState) => state.contextMenu)
  const dispatch = useDispatch();
  const [visible, setVisible] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });

  useEffect(() => {
    setVisible(state.visible)
    const position = state.position
    if (position) {
      setPosition({top: position.y, left: position.x}) 
    }
  }, [state])

  useEffect(() => {
    const handleClick = () => dispatch(setContextMenuVisible(false));
    window.addEventListener("click", handleClick);
    return () => {
      window.removeEventListener("click", handleClick);
    };
  }, [dispatch]);

  return (
    <>
      {visible && (
            <Box className="contextmenu" sx={{
              top: position.top,
              left: position.left,
              backgroundColor: muiTheme.palette.action.selected,
            }}>
              { actionItems.map((item, index)  => (
                <Box className="contextMenuItem" display="flex" key={index} onClick={() => {
                  onContextMenuItemClicked?.selected(item.actionIndex, row)
                }} >
                  {item.icon}
                  <Typography>{item.text}</Typography>
                </Box>
              ))}
              {actionItems.length === 0 && (
                <Typography>Nothing to do..</Typography>
              )}
            </Box>
      )}
    </>
  )
}
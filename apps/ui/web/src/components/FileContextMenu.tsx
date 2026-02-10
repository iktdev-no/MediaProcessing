import { Menu, MenuItem } from "@mui/material";
import type { FileAction, IFile, MediaAction } from "../types/types";

export interface FileContextMenuProps {
    file: IFile | null
    position: { mouseX: number; mouseY: number } | null
    onClose: () => void
    onMediaAction: (action: MediaAction, file: IFile) => void
    onFileAction: (action: FileAction, file: IFile) => void
    onCopyPath: (file: IFile) => void
}

export function FileContextMenu({
    file,
    position,
    onClose,
    onMediaAction,
    onFileAction,
    onCopyPath
}: FileContextMenuProps) {
    if (!file) return null

    return (
        <Menu
            open={!!position}
            onClose={onClose}
            anchorReference="anchorPosition"
            anchorPosition={
                position
                    ? { top: position.mouseY, left: position.mouseX }
                    : undefined
            }
        >
            {/* Media actions */}
            {file.actions.mediaActions.map(action => (
                <MenuItem
                    key={action.id}
                    onClick={() => onMediaAction(action, file)}
                >
                    {action.title}
                </MenuItem>
            ))}

            {/* File actions */}
            {file.actions.fileActions.map(action => (
                <MenuItem
                    key={action.id}
                    onClick={() => onFileAction(action, file)}
                    sx={action.id === "Delete" ? { color: "error.main" } : undefined}
                >
                    {action.title}
                </MenuItem>
            ))}

            {/* Always available */}
            <MenuItem onClick={() => onCopyPath(file)}>Kopier sti</MenuItem>
        </Menu>
    )
}

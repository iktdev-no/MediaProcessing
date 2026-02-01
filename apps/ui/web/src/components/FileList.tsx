import FolderIcon from "@mui/icons-material/Folder"
import InsertDriveFileIcon from "@mui/icons-material/InsertDriveFile"
import { List, ListItemButton, ListItemIcon, ListItemText } from "@mui/material"
import type { IFile } from "../types/files"
import { normalDate } from "../util"

export interface FileListProps {
    files: IFile[]
    onOpenFolder: (file: IFile) => void
    onContextMenu: (event: React.MouseEvent<HTMLElement>, file: IFile) => void
}

export function FileList({ files, onOpenFolder, onContextMenu }: FileListProps) {
    return (
        <List sx={{ bgcolor: "background.paper" }}>
            {files.map((f) => (
                <ListItemButton
                    key={f.uri}
                    onClick={() => f.type === "Folder" && onOpenFolder(f)}
                    onContextMenu={(e) => onContextMenu(e, f)}
                >
                    <ListItemIcon>
                        {f.type === "Folder" ? <FolderIcon /> : <InsertDriveFileIcon />}
                    </ListItemIcon>
                    <ListItemText
                        primary={f.name}
                        secondary={normalDate.format(new Date(f.created))}
                    />
                </ListItemButton>
            ))}
        </List>
    )
}

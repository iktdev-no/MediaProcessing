import DataObjectIcon from '@mui/icons-material/DataObject'
import FolderIcon from "@mui/icons-material/Folder"
import ImageIcon from '@mui/icons-material/Image'
import InsertDriveFileIcon from "@mui/icons-material/InsertDriveFile"
import MovieIcon from '@mui/icons-material/Movie'
import SubtitlesIcon from '@mui/icons-material/Subtitles'
import { List, ListItemButton, ListItemIcon, ListItemText } from "@mui/material"
import type { FileItem, IFile } from '../types/types'
import { normalDate } from "../util"

export interface FileListProps {
    files: IFile[]
    onOpenFolder: (file: IFile) => void
    onContextMenu: (event: React.MouseEvent<HTMLElement>, file: IFile) => void
}

export function FileList({ files, onOpenFolder, onContextMenu }: FileListProps) {
    const videoExtensions = ["mp4", "mkv", "mov", "avi", "webm", "ts", "m2ts"];
    const subtitleExtensions = ["srt", "ass", "vtt", "smi"];
    const pictureExtensions = [
        "webp", "png", "jpeg", "jpg",
        "avif", "heic", "heif", "bmp", "tiff", "tif"
    ]


    const getItemIcon = (file: IFile) => {
        if (file.type === "Folder") {
            return <FolderIcon sx={{ color: "#fbc02d" }} />
        }

        const f = file as FileItem
        const ext = f.extension.toLowerCase()

        if (videoExtensions.includes(ext)) {
            return <MovieIcon sx={{ color: "#42a5f5" }} />
        }

        if (subtitleExtensions.includes(ext)) {
            return <SubtitlesIcon sx={{ color: "#66bb6a" }} />
        }

        if (pictureExtensions.includes(ext)) {
            return <ImageIcon sx={{ color: "#26a69a" }} />
        }

        if (ext === "json") {
            return <DataObjectIcon sx={{ color: "#ab47bc" }} />
        }

        return <InsertDriveFileIcon sx={{ color: "#bdbdbd" }} />
    }




    return (
        <List sx={{ bgcolor: "background.paper" }}>
            {files.map((f) => (
                <ListItemButton
                    key={f.uri}
                    onClick={() => f.type === "Folder" && onOpenFolder(f)}
                    onContextMenu={(e) => onContextMenu(e, f)}
                >
                    <ListItemIcon>
                        {getItemIcon(f)}
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

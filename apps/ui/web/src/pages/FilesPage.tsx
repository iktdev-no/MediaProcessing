import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward"
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward"
import {
    Box,
    IconButton,
    Stack,
    ToggleButton,
    ToggleButtonGroup,
    Typography
} from "@mui/material"
import { useCallback, useEffect, useMemo, useState, type MouseEvent } from "react"
import { useSearchParams } from "react-router-dom"
import { apiDelete, apiGet } from "../api/client"

import { toast } from "react-toastify"
import { startProcess } from "../api/media"
import { BreadcrumbPath } from "../components/BreadcrumbPath"
import { ConfirmationDialog } from "../components/ConfirmationDialog"
import { FileContextMenu } from "../components/FileContextMenu"
import { FileList } from "../components/FileList"
import { LoadingToast } from "../components/LoadingToast"
import type { FileAction, IFile, MediaAction } from "../types/types"

/* ───────────────── Helpers ───────────────── */

type SortKey = "name" | "created" | "type"
type SortDir = "asc" | "desc"


/* ───────────────── Component ───────────────── */

export default function FilesPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const path = searchParams.get("path") ?? "/"

    const [files, setFiles] = useState<IFile[]>([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const [visible, setVisible] = useState<Record<string, boolean>>({})

    const [collapseVisible, setCollapseVisible] = useState<Record<string, boolean>>({})

    const [sortKey, setSortKey] = useState<SortKey>("name")
    const [sortDir, setSortDir] = useState<SortDir>("asc")

    const [menuPos, setMenuPos] = useState<{ mouseX: number; mouseY: number } | null>(null)
    const [menuFile, setMenuFile] = useState<IFile | null>(null)

    const [confirmOpen, setConfirmOpen] = useState(false)
    const [confirmTarget, setConfirmTarget] = useState<IFile | null>(null)

    /* ───── Data loading tied to URL ───── */

    const load = useCallback(
        async (path: string, push = true) => {
            try {
                setLoading(true)
                setError(null)

                const endpoint =
                    path === "/" ? "/files/roots" : `/files/explore?path=${encodeURIComponent(path)}`
                const data = await apiGet<IFile[]>(endpoint)
                setFiles(data)
                const map = Object.fromEntries(data.map(f => [f.uri, true]))
                setVisible(map)
                setCollapseVisible(map)
                if (push) setSearchParams({ path })
            } catch {
                setError("Kunne ikke laste mappe")
            } finally {
                setLoading(false)
            }
        },
        [setSearchParams]
    )

    useEffect(() => {
        load(path, false)
    }, [path, load])

    /* ───── Sorting ───── */

    const sortedFiles = useMemo(() => {
        const copy = [...files]
        copy.sort((a, b) => {
            if (a.type !== b.type) return a.type === "Folder" ? -1 : 1

            let res = 0
            switch (sortKey) {
                case "name":
                    res = a.name.localeCompare(b.name, undefined, { numeric: true })
                    break
                case "created":
                    res = a.created - b.created
                    break
                case "type":
                    res = a.type.localeCompare(b.type)
                    break
            }
            return sortDir === "asc" ? res : -res
        })
        return copy
    }, [files, sortKey, sortDir])

    /* ───── Context menu ───── */

    const openMenu = (e: MouseEvent<HTMLElement>, file: IFile) => {
        e.preventDefault()
        setMenuFile(file)
        setMenuPos({
            mouseX: e.clientX + 2,
            mouseY: e.clientY + 4,
        })
    }

    const closeMenu = () => {
        setMenuPos(null)
        setMenuFile(null)
    }

    const onCopyPath = (file: IFile) => {
        navigator.clipboard.writeText(file.uri)
        closeMenu()
    }

    const onMediaAction = async (action: MediaAction, file: IFile) => {

        console.log("MEDIA ACTION:", action, file)
        closeMenu()
        try {
            await startProcess({ fileUri: file.uri, mediaAction: [action.id] })
            console.log("Started process:", action.id, "for", file.uri)
        } catch (err) {
            console.error("Failed to start process", err)
        }
    }

    const onFileAction = (action: FileAction, file: IFile) => {
        console.log("FILE ACTION:", action, file)
        if (action.id === "Open" && file.type === "Folder") {
            console.log("OPEN FOLDER:", file)
            load(file.uri)
            closeMenu()
            return
        }

        if (action.id === "Delete") {
            setConfirmTarget(file)
            setConfirmOpen(true)
            closeMenu()
            return
        }

        closeMenu()
    }

    const FILE_FADE_DURATION = getCssDurationVar("--filefade-duration")
    function getCssDurationVar(name: string): number {
        const raw = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
        return raw.endsWith("ms")
            ? parseFloat(raw)
            : raw.endsWith("s")
                ? parseFloat(raw) * 1000
                : Number(raw)
    }


    const onDelete = async (item: IFile | null) => {
        if (!item) return
        setLoading(true)

        try {
            await apiDelete("/files/delete", {
                body: { uri: item.uri },
                onError: () => { }
            })
            console.log("Deleted:", item)
            toast.success(`Deleted ${item.uri}`)
            // 1. Trigger CSS slide/fade/rød animasjon
            setVisible(prev => ({ ...prev, [item.uri]: false }))

            // 2. Etter CSS-animasjonen → Collapse får lov å kollapse høyden
            setTimeout(() => {
                setCollapseVisible(prev => ({ ...prev, [item.uri]: false }))
            }, FILE_FADE_DURATION)

            // 3. Etter Collapse → fjern elementet fra DOM
            setTimeout(() => {
                setFiles(prev => prev.filter(f => f.uri !== item.uri))
            }, FILE_FADE_DURATION * 2)
        } catch (err) {
            toast.error(`Failed to delete ${item.uri}`)
            console.error("Delete failed", err)
        } finally {
            setLoading(false)
            setConfirmOpen(false)
        }
    }

    if (error) return <Typography color="error">{error}</Typography>

    return (
        <Box sx={{ height: "100%", width: "100%", display: "flex", flexDirection: "column", overflow: "hidden" }}>
            <Box sx={{ flex: 1, overflow: "auto", width: "100%" }}>
                {/* Sticky header */}
                <Box
                    sx={{
                        position: "sticky",
                        top: 0,
                        zIndex: 20,
                        pt: 1,
                        bgcolor: "background.paper",
                        borderBottom: 1,
                        borderColor: "divider",
                    }}
                >
                    <Stack direction="row" alignItems="center" spacing={2} p={1}>
                        <Stack direction="row" alignItems="center" spacing={1} flex={1}>

                            <BreadcrumbPath path={path} onNavigate={load} />

                        </Stack>

                        <ToggleButtonGroup
                            size="small"
                            value={sortKey}
                            exclusive
                            onChange={(_, v) => v && setSortKey(v)}
                        >
                            <ToggleButton value="name">Navn</ToggleButton>
                            <ToggleButton value="created">Dato</ToggleButton>
                            <ToggleButton value="type">Type</ToggleButton>
                        </ToggleButtonGroup>

                        <IconButton onClick={() => setSortDir(d => d === "asc" ? "desc" : "asc")}>
                            {sortDir === "asc" ? <ArrowUpwardIcon /> : <ArrowDownwardIcon />}
                        </IconButton>
                    </Stack>
                </Box>

                {/* File list */}
                <FileList
                    files={sortedFiles}
                    visible={visible}
                    onOpenFolder={(file) => load(file.uri)}
                    onContextMenu={openMenu}
                    collapseVisible={collapseVisible}
                    FILE_FADE_DURATION={FILE_FADE_DURATION}
                />

            </Box>

            {/* Context menu */}
            <FileContextMenu
                file={menuFile}
                position={menuPos}
                onClose={closeMenu}
                onMediaAction={onMediaAction}
                onFileAction={onFileAction}
                onCopyPath={onCopyPath}
            />

            {/* Delete confirmation */}
            <ConfirmationDialog
                open={confirmOpen}
                title="Slette fil?"
                message={`Vil du slette ${confirmTarget?.name} ? `}
                confirmLabel="Slett"
                confirmColor="error"
                onCancel={() => setConfirmOpen(false)}
                onConfirm={() => onDelete(confirmTarget)}
            />
            <LoadingToast open={loading} />

        </Box>
    )
}

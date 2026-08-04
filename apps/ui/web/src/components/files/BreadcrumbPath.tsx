import HomeIcon from "@mui/icons-material/Home"
import { Breadcrumbs, Chip } from "@mui/material"

export interface BreadcrumbPathProps {
    path: string
    onNavigate: (path: string) => void
}

function splitPath(path: string): string[] {
    if (!path || path === "/") return []
    return path.split("/").filter(Boolean)
}

function buildPath(parts: string[], index: number) {
    return "/" + parts.slice(0, index + 1).join("/")
}

export function BreadcrumbPath({ path, onNavigate }: BreadcrumbPathProps) {
    const segments = splitPath(path)

    return (
        <>
            <Chip
                icon={<HomeIcon />}
                label="Home"
                clickable
                onClick={() => onNavigate("/")}
                sx={{ fontWeight: 600 }}
            />
            <Breadcrumbs separator="›">
                <Chip
                    label={"/"}
                    clickable
                    onClick={() => onNavigate("/")}
                    variant="outlined"
                    sx={{ fontWeight: 500 }}
                />

                {segments.map((segment, index) => {
                    const p = buildPath(segments, index)
                    return (
                        <Chip
                            key={p}
                            label={segment}
                            clickable
                            onClick={() => onNavigate(p)}
                            variant="outlined"
                            sx={{ fontWeight: 500 }}
                        />
                    )
                })}
            </Breadcrumbs>
        </>

    )
}

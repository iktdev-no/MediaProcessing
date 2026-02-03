import type { IFile } from "../types/files"
import { apiGet } from "./client"

export function apiListHome() {
    return apiGet<IFile[]>("/files/home")
}

export function apiExplore(path: string) {
    return apiGet<IFile[]>(`/files/explore?path=${encodeURIComponent(path)}`)
}
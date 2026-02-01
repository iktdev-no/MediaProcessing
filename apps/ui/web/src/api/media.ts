import type { StartProcessRequest } from "../types/files"
import { apiPost } from "./client"

export function startProcess(req: StartProcessRequest) {
    return apiPost<StartProcessRequest, Record<string, string>>(
        "/operations/start",
        req
    )
}

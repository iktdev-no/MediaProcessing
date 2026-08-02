import type { UiFile, PreservedFile } from "../../types/types";
import { apiGet, apiPut } from "../client";

export function apiListHome() {
  return apiGet<UiFile[]>("/files/home");
}

export function apiExplore(path: string) {
  return apiGet<UiFile[]>(`/files/explore?path=${encodeURIComponent(path)}`);
}

export function getUsedFiles() {
  return apiGet<PreservedFile[]>("/files/used");
}

export function putPreservedFiles(fileUris: string[]) {
  return apiPut<string[], PreservedFile[]>("/files/preserve", fileUris);
}

import type { UiFile, PreservedFile, UiFileRef } from "../../types/types";
import { apiGet, apiPut } from "../client";

export function apiListHome() {
  return apiGet<UiFileRef[]>("/files/home");
}

export function apiExplore(path: string, notInUse: boolean) {
  if (notInUse) {
    return apiGet<UiFileRef[]>(`/files/explore?new=${notInUse}&path=${encodeURIComponent(path)}`);
  } else {
    return apiGet<UiFileRef[]>(`/files/explore?path=${encodeURIComponent(path)}`);
  }
}

export function getUsedFiles() {
  return apiGet<PreservedFile[]>("/files/used");
}

export function putPreservedFiles(fileUris: string[]) {
  return apiPut<string[], PreservedFile[]>("/files/preserve", fileUris);
}

import type { InputFileInfo } from "../../types/transfer-model";
import type { IUiFile } from "../../types/types";
import { apiGet, apiPut } from "../client";

export function apiListHome() {
  return apiGet<IUiFile[]>("/files/home");
}

export function apiExplore(path: string) {
  return apiGet<IUiFile[]>(`/files/explore?path=${encodeURIComponent(path)}`);
}

export function getUsedFiles() {
  return apiGet<InputFileInfo[]>("/files/used");
}

export function putPreservedFiles(fileUris: string[]) {
  return apiPut<string[], InputFileInfo[]>("/files/preserve", fileUris);
}

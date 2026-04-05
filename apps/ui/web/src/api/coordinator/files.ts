import type { IUiFile } from "../../types/types";
import { apiGet } from "../client";

export function apiListHome() {
  return apiGet<IUiFile[]>("/files/home");
}

export function apiExplore(path: string) {
  return apiGet<IUiFile[]>(`/files/explore?path=${encodeURIComponent(path)}`);
}

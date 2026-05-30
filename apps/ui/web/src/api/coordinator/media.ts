import type { StartProcessRequest } from "../../types/types";
import { apiPost } from "../client";

export function startProcess(req: StartProcessRequest) {
  return apiPost<StartProcessRequest, Record<string, string>>(
    "/operations/start",
    req,
  );
}

export function triggerCacheCleanup() {
  return apiPost<void, void>(
    "/operations/cleanup/cache",
    undefined // Ingen body nødvendig for denne operasjonen
  );
}

export function triggerCacheWipe() {
  return apiPost<void, void>(
    "/operations/cleanup/cache/wipe",
    undefined // Ingen body nødvendig for denne operasjonen
  );
}



export function triggerInboxCleanup() {
  return apiPost<void, void>(
    "/operations/cleanup/inbox",
    undefined
  );
}

export function triggerInboxWipe() {
  return apiPost<void, void>(
    "/operations/cleanup/inbox/wipe",
    undefined
  );
}
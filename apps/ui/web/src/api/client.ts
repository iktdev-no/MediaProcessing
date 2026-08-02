import { toast } from "react-toastify";

// ------------------------------------------------------------
// GET
// ------------------------------------------------------------
export async function apiGet<T>(
  path: string,
  opts?: {
    onError?: (status: number, body: any) => void;
  },
): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: {
      Accept: "application/json",
    },
  });

  if (!res.ok) {
    const status = res.status;
    let body: any = null;

    try {
      body = await res.json();
    } catch {
      body = await res.text().catch(() => null);
    }

    // Custom handler?
    if (opts?.onError) {
      opts.onError(status, body);
      return Promise.reject({ status, body });
    }

    // ⭐ Automatic toast
    const message =
      typeof body === "object" && body?.message
        ? body.message
        : `GET ${path} failed with ${status}`;

    toast.error(message);

    const error: any = new Error(message);
    error.status = status;
    error.body = body;
    throw error;
  }

  return res.json();
}

// ------------------------------------------------------------
// POST
// ------------------------------------------------------------
export async function apiPost<TRequest, TResponse>(
  path: string,
  body: TRequest,
): Promise<TResponse> {
  const res = await fetch(`/api${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "*/*",
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    let errorBody: any = null;

    try {
      errorBody = await res.json();
    } catch {
      errorBody = await res.text().catch(() => null);
    }

    const message =
      typeof errorBody === "object" && errorBody?.message
        ? errorBody.message
        : `POST ${path} failed with ${res.status}`;

    toast.error(message);

    const error: any = new Error(message);
    error.status = res.status;
    error.body = errorBody;
    throw error;
  }

  const contentType = res.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    return res.json();
  }

  const text = await res.text();
  return text as unknown as TResponse;
}

// ------------------------------------------------------------
// DELETE
// ------------------------------------------------------------
export async function apiDelete<TResponse>(
  path: string,
  opts?: {
    body?: any;
    onError?: (status: number, body: any) => void;
  },
): Promise<TResponse> {
  const res = await fetch(`/api${path}`, {
    method: "DELETE",
    headers: {
      Accept: "*/*",
      ...(opts?.body ? { "Content-Type": "application/json" } : {}),
    },
    body: opts?.body ? JSON.stringify(opts.body) : undefined,
  });

  if (!res.ok) {
    const status = res.status;
    let body: any = null;

    try {
      body = await res.json();
    } catch {
      body = await res.text().catch(() => null);
    }

    // ⭐ If caller handles error → do NOT toast
    if (opts?.onError) {
      opts.onError(status, body);
      throw { status, body };
    }

    // ⭐ Automatic toast only when no handler is provided
    const message =
      typeof body === "object" && body?.message
        ? body.message
        : `DELETE ${path} failed with ${status}`;

    toast.error(message);

    const error: any = new Error(message);
    error.status = status;
    error.body = body;
    throw error;
  }

  return res.json().catch(() => null);
}

// ------------------------------------------------------------
// PATCH
// ------------------------------------------------------------
export async function apiPatch<TRequest, TResponse>(
  path: string,
  body: TRequest,
  opts?: {
    onError?: (status: number, body: any) => void;
  },
): Promise<TResponse> {
  const res = await fetch(`/api${path}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Accept: "*/*",
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const status = res.status;
    let errorBody: any = null;

    try {
      errorBody = await res.json();
    } catch {
      errorBody = await res.text().catch(() => null);
    }

    // Custom error handler?
    if (opts?.onError) {
      opts.onError(status, errorBody);
      throw { status, body: errorBody };
    }

    // Automatic toast
    const message =
      typeof errorBody === "object" && errorBody?.message
        ? errorBody.message
        : `PATCH ${path} failed with ${status}`;

    toast.error(message);

    const error: any = new Error(message);
    error.status = status;
    error.body = errorBody;
    throw error;
  }

  // Handle empty body (204 No Content)
  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return res.json();
  }

  const text = await res.text().catch(() => null);
  return text as unknown as TResponse;
}

// ------------------------------------------------------------
// PUT
// ------------------------------------------------------------
export async function apiPut<TRequest, TResponse>(
  path: string,
  body: TRequest,
): Promise<TResponse> {
  const res = await fetch(`/api${path}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Accept: "*/*",
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    let errorBody: any = null;

    try {
      errorBody = await res.json();
    } catch {
      errorBody = await res.text().catch(() => null);
    }

    const message =
      typeof errorBody === "object" && errorBody?.message
        ? errorBody.message
        : `POST ${path} failed with ${res.status}`;

    toast.error(message);

    const error: any = new Error(message);
    error.status = res.status;
    error.body = errorBody;
    throw error;
  }

  const contentType = res.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    return res.json();
  }

  const text = await res.text();
  return text as unknown as TResponse;
}


// ------------------------------------------------------------
// Query builder
// ------------------------------------------------------------
export function buildQuery(params: Record<string, any>): string {
  const search = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) continue;

    if (Array.isArray(value)) {
      value.forEach((v) => search.append(key, String(v)));
    } else {
      search.append(key, String(value));
    }
  }

  return search.toString();
}

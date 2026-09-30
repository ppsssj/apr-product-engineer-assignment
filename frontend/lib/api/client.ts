import type { ApiErrorBody } from "./types";

const DEFAULT_API_BASE_URL = "http://localhost:8080/api";

export const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL || DEFAULT_API_BASE_URL
).replace(/\/+$/, "");

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

type QueryValue = string | number | undefined;

type RequestOptions = {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  query?: Record<string, QueryValue>;
  body?: unknown;
  // Lets React Query cancel superseded reads (changed search/sort, unmount).
  signal?: AbortSignal;
};

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) {
      params.set(key, String(value));
    }
  }
  const search = params.toString();
  return `${API_BASE_URL}${normalizedPath}${search ? `?${search}` : ""}`;
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as ApiErrorBody).code === "string" &&
    typeof (value as ApiErrorBody).message === "string"
  );
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body: unknown = await response.json();
    if (isApiErrorBody(body)) {
      return new ApiError(response.status, body.code, body.message);
    }
  } catch {
    // Non-JSON or empty error body: fall through to the generic error.
  }
  return new ApiError(
    response.status,
    "HTTP_ERROR",
    `Request failed with status ${response.status}`,
  );
}

// Single fetch entry point for the backend API. Every request carries
// credentials so the browser sends the `x-user-id` cookie set by /dev-user.
export async function request<T>(
  path: string,
  { method = "GET", query, body, signal }: RequestOptions = {},
): Promise<T> {
  const hasBody = body !== undefined;
  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      credentials: "include",
      cache: "no-store",
      headers: hasBody
        ? { Accept: "application/json", "Content-Type": "application/json" }
        : { Accept: "application/json" },
      body: hasBody ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (error) {
    // A cancelled request is not a network failure: rethrow the abort as-is
    // so React Query treats it as a cancellation, never as an error state.
    if (signal?.aborted) throw error;
    throw new ApiError(0, "NETWORK_ERROR", "Network request failed");
  }

  if (!response.ok) {
    throw await toApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  try {
    return (await response.json()) as T;
  } catch {
    throw new ApiError(
      response.status,
      "INVALID_RESPONSE",
      "Response body is not valid JSON",
    );
  }
}

import "server-only";
import type { InitialPostPage, PageResponse, Post } from "./types";

// Server Components reach the backend over the internal network (Compose:
// `API_BASE_URL=http://backend:8080/api`); the browser-facing
// NEXT_PUBLIC_API_BASE_URL (localhost:8080) is only a fallback for local runs.
const SERVER_API_BASE_URL = (
  process.env.API_BASE_URL ||
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  "http://localhost:8080/api"
).replace(/\/+$/, "");

// The first-page render is an optimization: a slow backend must not hold the
// HTML longer than this.
const SERVER_FETCH_TIMEOUT_MS = 2000;

// First page of the default list (latest, no search) for the requester of the
// incoming request. `requester` is the decoded `x-user-id` cookie value; it is
// re-encoded so the backend sees exactly the value it would read from the
// browser's cookie (/dev-user stores it with encodeURIComponent), and sent as
// the `x-user-id` header the backend resolves first. Without a cookie no header
// is sent and the backend applies its default requester, as for the browser.
// Any failure returns null so the page falls back to the client fetch.
export async function getInitialPostPage(
  limit: number,
  requester: string | undefined,
): Promise<InitialPostPage | null> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (requester?.trim()) headers["x-user-id"] = encodeURIComponent(requester);

  try {
    const response = await fetch(`${SERVER_API_BASE_URL}/posts?limit=${limit}&sort=latest`, {
      headers,
      cache: "no-store",
      signal: AbortSignal.timeout(SERVER_FETCH_TIMEOUT_MS),
    });
    if (!response.ok) return null;
    const page = (await response.json()) as PageResponse<Post>;
    if (!Array.isArray(page?.items)) return null;
    return { page, fetchedAt: Date.now() };
  } catch {
    return null;
  }
}

const REQUESTER_COOKIE = "x-user-id";
export const DEFAULT_REQUESTER = "apr_tester";

// The browser sends only the `x-user-id` cookie set by /dev-user (never a
// header), so the backend resolves the first non-blank cookie value, trimmed,
// or falls back to `apr_tester`. This mirrors that rule for presentation only
// (e.g. showing a delete affordance); the backend still enforces ownership.
export function readCurrentRequester(): string {
  if (typeof document === "undefined") return DEFAULT_REQUESTER;
  for (const part of document.cookie.split(";")) {
    const separator = part.indexOf("=");
    if (separator < 0 || part.slice(0, separator).trim() !== REQUESTER_COOKIE) {
      continue;
    }
    let value = part.slice(separator + 1);
    try {
      value = decodeURIComponent(value);
    } catch {
      // Keep the raw value if it is not valid percent-encoding.
    }
    if (value.trim()) return value.trim();
  }
  return DEFAULT_REQUESTER;
}

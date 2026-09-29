const formatter = new Intl.RelativeTimeFormat("ko", { numeric: "always" });

const MINUTE = 60;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

// Formats an ISO timestamp relative to `now` (e.g. "3분 전", "2일 전").
// Returns null for an unparseable timestamp instead of rendering "NaN".
export function formatRelativeTime(
  createdAt: string,
  now: Date | number = Date.now(),
): string | null {
  const then = Date.parse(createdAt);
  const current = typeof now === "number" ? now : now.getTime();
  if (Number.isNaN(then) || Number.isNaN(current)) {
    return null;
  }

  const diffSeconds = Math.round((current - then) / 1000);
  const elapsed = Math.abs(diffSeconds);
  // Past values are negative ("… 전"); future values are positive ("… 후").
  const sign = diffSeconds >= 0 ? -1 : 1;

  if (elapsed < MINUTE) return formatter.format(sign * elapsed, "second");
  if (elapsed < HOUR)
    return formatter.format(sign * Math.floor(elapsed / MINUTE), "minute");
  if (elapsed < DAY)
    return formatter.format(sign * Math.floor(elapsed / HOUR), "hour");

  const days = Math.floor(elapsed / DAY);
  if (days < 30) return formatter.format(sign * days, "day");
  if (days < 365) return formatter.format(sign * Math.floor(days / 30), "month");
  return formatter.format(sign * Math.floor(days / 365), "year");
}

// Formats an ISO timestamp as "YY.MM.DD" in the viewer's local time zone
// (e.g. "25.08.08"), the post-date format shown in the detail design.
// Returns null for an unparseable timestamp.
export function formatShortDate(createdAt: string): string | null {
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  const yy = String(date.getFullYear() % 100).padStart(2, "0");
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yy}.${mm}.${dd}`;
}

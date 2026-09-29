// One-shot message handed from one screen to the next within the same page
// load (e.g. create → list toast). It lives in module memory: a client-side
// navigation keeps it, a reload or a new tab starts empty, and reading it
// clears it, so each create shows its toast exactly once.

let pending: string | null = null;

export function setFlash(message: string): void {
  pending = message;
}

export function takeFlash(): string | null {
  const message = pending;
  pending = null;
  return message;
}

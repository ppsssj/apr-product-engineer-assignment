// Remembers which in-app screen pushed a form (list FAB → create, detail
// sheet → edit). After a successful submit the form can go back to that
// history entry instead of stacking a duplicate one, so Back from the updated
// detail returns to the list. Module memory: a reload or a direct URL starts
// empty, and the form then replaces its own entry instead.

let origin: string | null = null;

export function rememberFormOrigin(path: string): void {
  origin = path;
}

// True (and cleared) when the form was opened from `path`.
export function takeFormOrigin(path: string): boolean {
  const matches = origin === path;
  origin = null;
  return matches;
}

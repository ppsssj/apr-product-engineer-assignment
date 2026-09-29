// Route segment → post ID. Only positive safe integers are valid; anything
// else renders the not-found state without an API request.
export function parsePostId(segment: string): number | null {
  return /^[1-9]\d*$/.test(segment) && Number.isSafeInteger(Number(segment))
    ? Number(segment)
    : null;
}

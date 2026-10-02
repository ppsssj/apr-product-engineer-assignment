// Shared by the server-rendered home page and the client list hook: values
// exported from a "use client" module are not usable in Server Components.
export const POST_LIST_PAGE_SIZE = 15;

// How far below the viewport the list sentinel starts the next-page request.
// A page takes ≈0.65s from request to rows on throttled mobile; 500px covers
// that at a ≈600px/s skim (docs/PERFORMANCE.md §14). Comment continuation keeps
// the hook's default.
export const POST_LIST_PREFETCH_ROOT_MARGIN = "0px 0px 500px 0px";

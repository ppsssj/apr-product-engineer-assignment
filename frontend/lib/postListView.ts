import type { PostSort } from "@/lib/api/types";

// The list's last sort/search, kept across in-app navigation (list → detail →
// back) so returning does not silently reset the chosen view. Module memory
// like lib/flash.ts: a reload or a new tab starts from the default view.
// PostListScreen still owns the state; this only seeds it on remount.
export type PostListView = {
  sort: PostSort;
  searchOpen: boolean;
  query: string | null;
};

const DEFAULT_VIEW: PostListView = { sort: "latest", searchOpen: false, query: null };

let lastView: PostListView = DEFAULT_VIEW;

export function readPostListView(): PostListView {
  return lastView;
}

export function rememberPostListView(view: PostListView): void {
  lastView = view;
}

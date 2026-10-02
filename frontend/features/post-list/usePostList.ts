"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { getPosts } from "@/lib/api/community";
import type { InitialPostPage, PostSort } from "@/lib/api/types";
import { normalizePostQuery, postKeys } from "@/lib/query/keys";
import { POST_LIST_PAGE_SIZE } from "./postListConfig";

// A server-rendered first page stays fresh for this long so hydration does not
// refetch what the HTML already shows. Once the client has fetched the list
// itself (dataUpdateCount > 0) the default staleTime of 0 applies as before.
const SEED_FRESH_MS = 5_000;

// One infinite query per (page size, sort, search). The opaque backend cursor
// is the pageParam, so a new sort or search always starts from its own first
// page and never reuses another list's cursor. React Query's signal cancels
// the request when the key changes or the screen unmounts.
// `initialPage` seeds only the default view (latest, no search). As initialData
// it fills an empty cache entry and never replaces one that already exists.
export function usePostList(
  sort: PostSort,
  query: string | null,
  initialPage?: InitialPostPage | null,
) {
  const normalizedQuery = normalizePostQuery(query);
  const seed = sort === "latest" && normalizedQuery === null ? initialPage : null;
  return useInfiniteQuery({
    queryKey: postKeys.list({ limit: POST_LIST_PAGE_SIZE, sort, query: normalizedQuery }),
    queryFn: ({ pageParam, signal }) =>
      getPosts(
        { limit: POST_LIST_PAGE_SIZE, cursor: pageParam, sort, query: normalizedQuery ?? undefined },
        signal,
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    // Spread only when seeded: an `initialData` key, even undefined, would
    // type the result as always defined and hide the pending state.
    ...(seed
      ? {
          initialData: { pages: [seed.page], pageParams: [undefined] },
          initialDataUpdatedAt: seed.fetchedAt,
        }
      : {}),
    staleTime: (cached) => (cached.state.dataUpdateCount === 0 ? SEED_FRESH_MS : 0),
  });
}

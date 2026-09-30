"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { getPosts } from "@/lib/api/community";
import type { PostSort } from "@/lib/api/types";
import { normalizePostQuery, postKeys } from "@/lib/query/keys";

export const POST_LIST_PAGE_SIZE = 10;

// One infinite query per (page size, sort, search). The opaque backend cursor
// is the pageParam, so a new sort or search always starts from its own first
// page and never reuses another list's cursor. React Query's signal cancels
// the request when the key changes or the screen unmounts.
export function usePostList(sort: PostSort, query: string | null) {
  const normalizedQuery = normalizePostQuery(query);
  return useInfiniteQuery({
    queryKey: postKeys.list({ limit: POST_LIST_PAGE_SIZE, sort, query: normalizedQuery }),
    queryFn: ({ pageParam, signal }) =>
      getPosts(
        { limit: POST_LIST_PAGE_SIZE, cursor: pageParam, sort, query: normalizedQuery ?? undefined },
        signal,
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
  });
}

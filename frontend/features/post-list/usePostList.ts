"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { getPosts } from "@/lib/api/community";
import { postKeys } from "@/lib/query/keys";

export const POST_LIST_PAGE_SIZE = 10;

// Newest-first post list. The opaque backend cursor is the pageParam, so every
// page of this list lives under one query key.
export function usePostList() {
  return useInfiniteQuery({
    queryKey: postKeys.list({ limit: POST_LIST_PAGE_SIZE }),
    queryFn: ({ pageParam }) =>
      getPosts({ limit: POST_LIST_PAGE_SIZE, cursor: pageParam }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
  });
}

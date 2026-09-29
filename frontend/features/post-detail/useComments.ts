"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { getComments } from "@/lib/api/community";
import { postKeys } from "@/lib/query/keys";

// The detail screen shows at most 2 comments until "More" is activated.
export const INITIAL_COMMENT_COUNT = 2;
// Page size for the continuation after "More" (PRODUCT_DECISION; backend max 50).
export const COMMENT_PAGE_SIZE = 20;

type CommentPageParam = { cursor?: string; limit: number };

// Newest-first comments as one infinite query. The first page requests only
// `limit=2`; later pages continue from the keyset cursor with a larger limit,
// which the (createdAt, id) cursor supports independently of page size.
// The query never fetches a second page by itself: the screen calls
// `fetchNextPage` only after "More".
//
// `gcTime: 0` makes the "More" gate per visit: when the detail screen
// unmounts, the query (with any continuation pages) is dropped instead of
// staying in the shared QueryClient. A later visit therefore starts again from
// a single `limit=2` page, so neither a mount refetch nor "More" can bring back
// previously loaded continuation pages all at once.
export function useComments(postId: number) {
  return useInfiniteQuery({
    queryKey: postKeys.commentList(postId),
    queryFn: ({ pageParam }) =>
      getComments(postId, { limit: pageParam.limit, cursor: pageParam.cursor }),
    initialPageParam: { limit: INITIAL_COMMENT_COUNT } as CommentPageParam,
    getNextPageParam: (lastPage): CommentPageParam | undefined =>
      lastPage.nextCursor
        ? { cursor: lastPage.nextCursor, limit: COMMENT_PAGE_SIZE }
        : undefined,
    gcTime: 0,
  });
}

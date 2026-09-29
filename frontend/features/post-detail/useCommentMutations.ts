"use client";

import {
  useMutation,
  useQueryClient,
  type InfiniteData,
  type QueryClient,
} from "@tanstack/react-query";
import { createComment, deleteComment } from "@/lib/api/community";
import type { Comment, PageResponse, Post } from "@/lib/api/types";
import { useSingleFlight } from "@/lib/hooks/useSingleFlight";
import { postKeys } from "@/lib/query/keys";

type CommentPages = InfiniteData<PageResponse<Comment>>;

// Cache strategy (server-confirmed; nothing changes before the request succeeds):
// - The comment list cache is edited in place: a created comment is the newest
//   row, so it is prepended to the first page; a deleted comment is removed from
//   whichever page holds it. Every page's `total` moves by one. Existing
//   `nextCursor` values stay valid because a keyset cursor is a (createdAt, id)
//   bound, not a position, so no row is skipped or repeated.
// - The resulting `total` is copied as an absolute value into the post detail
//   and into every cached post-list page, so all three caches show one number.
// - If the comment list has not loaded, only the counts move (from the detail
//   cache) and the comment list is refetched from the server.

function setPostCommentCount(queryClient: QueryClient, postId: number, count: number) {
  queryClient.setQueryData<Post>(postKeys.detail(postId), (post) =>
    post ? { ...post, commentCount: count } : post,
  );
  queryClient.setQueriesData<InfiniteData<PageResponse<Post>>>(
    { queryKey: postKeys.lists() },
    (data) =>
      data && {
        ...data,
        pages: data.pages.map((page) => ({
          ...page,
          items: page.items.map((post) =>
            post.id === postId ? { ...post, commentCount: count } : post,
          ),
        })),
      },
  );
}

function detailCount(queryClient: QueryClient, postId: number): number | undefined {
  return queryClient.getQueryData<Post>(postKeys.detail(postId))?.commentCount;
}

export function useCreateComment(postId: number) {
  const queryClient = useQueryClient();
  const flight = useSingleFlight();

  const mutation = useMutation({
    mutationFn: (content: string) => createComment(postId, { content }),
    onSuccess: (comment) => {
      const key = postKeys.commentList(postId);
      const pages = queryClient.getQueryData<CommentPages>(key);
      if (pages && pages.pages.length > 0) {
        const alreadyListed = pages.pages.some((page) =>
          page.items.some((item) => item.id === comment.id),
        );
        const total = alreadyListed ? pages.pages[0].total : pages.pages[0].total + 1;
        if (!alreadyListed) {
          queryClient.setQueryData<CommentPages>(key, {
            ...pages,
            pages: pages.pages.map((page, index) => ({
              ...page,
              items: index === 0 ? [comment, ...page.items] : page.items,
              total,
            })),
          });
        }
        setPostCommentCount(queryClient, postId, total);
      } else {
        const count = detailCount(queryClient, postId);
        if (count !== undefined) setPostCommentCount(queryClient, postId, count + 1);
        void queryClient.invalidateQueries({ queryKey: key });
      }
    },
    onSettled: flight.end,
  });

  return {
    // Resolves true on success so the caller can clear its input.
    submit: async (content: string) => {
      if (!flight.begin()) return false;
      try {
        await mutation.mutateAsync(content);
        return true;
      } catch {
        return false;
      }
    },
    isPending: mutation.isPending,
    isError: mutation.isError,
    reset: mutation.reset,
  };
}

export function useDeleteComment(postId: number) {
  const queryClient = useQueryClient();
  const flight = useSingleFlight();

  const mutation = useMutation({
    mutationFn: (commentId: number) => deleteComment(commentId),
    onSuccess: (_result, commentId) => {
      const key = postKeys.commentList(postId);
      const pages = queryClient.getQueryData<CommentPages>(key);
      const listed = pages?.pages.some((page) =>
        page.items.some((item) => item.id === commentId),
      );
      if (pages && listed) {
        const total = pages.pages[0].total - 1;
        queryClient.setQueryData<CommentPages>(key, {
          ...pages,
          pages: pages.pages.map((page) => ({
            ...page,
            items: page.items.filter((item) => item.id !== commentId),
            total,
          })),
        });
        setPostCommentCount(queryClient, postId, total);
        // With only the first page loaded (the collapsed view), removing a row
        // can leave fewer than the two comments that should be visible, so the
        // first page is fetched again (one `limit=2` request).
        if (pages.pages.length === 1) {
          void queryClient.invalidateQueries({ queryKey: key });
        }
      } else {
        const count = detailCount(queryClient, postId);
        if (count !== undefined) setPostCommentCount(queryClient, postId, count - 1);
        void queryClient.invalidateQueries({ queryKey: key });
      }
    },
    onSettled: flight.end,
  });

  return {
    remove: async (commentId: number) => {
      if (!flight.begin()) return false;
      try {
        await mutation.mutateAsync(commentId);
        return true;
      } catch {
        return false;
      }
    },
    isPending: mutation.isPending,
    isError: mutation.isError,
    reset: mutation.reset,
  };
}

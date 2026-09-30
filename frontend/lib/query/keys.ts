// Query key factory. Keys hold only primitive, serializable values.
// `cursor` is intentionally excluded: infinite queries pass it as pageParam,
// so all pages of one list share a single key.

import type { PostSort } from "@/lib/api/types";

type ListKeyParams = { limit?: number };

// A post list is identified by its page size, ordering, and search filter:
// each combination is a distinct infinite-query cache.
export type PostListKeyParams = {
  limit?: number;
  sort?: PostSort;
  query?: string | null;
};

export type PostListKey = { limit: number | null; sort: PostSort; query: string | null };

export function normalizePostQuery(query: string | null | undefined): string | null {
  const trimmed = query?.trim();
  return trimmed ? trimmed : null;
}

export const postKeys = {
  all: ["posts"] as const,
  lists: () => [...postKeys.all, "list"] as const,
  list: (params: PostListKeyParams = {}) =>
    [
      ...postKeys.lists(),
      {
        limit: params.limit ?? null,
        sort: params.sort ?? "latest",
        query: normalizePostQuery(params.query),
      } satisfies PostListKey,
    ] as const,
  details: () => [...postKeys.all, "detail"] as const,
  detail: (postId: number) => [...postKeys.details(), postId] as const,
  comments: (postId: number) =>
    [...postKeys.detail(postId), "comments"] as const,
  commentList: (postId: number, params: ListKeyParams = {}) =>
    [...postKeys.comments(postId), { limit: params.limit ?? null }] as const,
};

// Reads the list parameters back from a cached post-list query key.
export function postListKeyParams(queryKey: readonly unknown[]): PostListKey | null {
  const params = queryKey[2];
  return typeof params === "object" && params !== null ? (params as PostListKey) : null;
}

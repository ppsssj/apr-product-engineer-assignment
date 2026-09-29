// Query key factory. Keys hold only primitive, serializable values.
// `cursor` is intentionally excluded: infinite queries pass it as pageParam,
// so all pages of one list share a single key.

type ListKeyParams = { limit?: number };

export const postKeys = {
  all: ["posts"] as const,
  lists: () => [...postKeys.all, "list"] as const,
  list: (params: ListKeyParams = {}) =>
    [...postKeys.lists(), { limit: params.limit ?? null }] as const,
  details: () => [...postKeys.all, "detail"] as const,
  detail: (postId: number) => [...postKeys.details(), postId] as const,
  comments: (postId: number) =>
    [...postKeys.detail(postId), "comments"] as const,
  commentList: (postId: number, params: ListKeyParams = {}) =>
    [...postKeys.comments(postId), { limit: params.limit ?? null }] as const,
};

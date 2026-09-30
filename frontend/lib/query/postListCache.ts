import type { InfiniteData, QueryClient, QueryKey } from "@tanstack/react-query";
import type { PageResponse, Post } from "@/lib/api/types";
import { postKeys, postListKeyParams, type PostListKey } from "./keys";

type PostPages = InfiniteData<PageResponse<Post>>;

// Post lists differ by sort and search, so they are no longer one shared
// collection. Each mutation edits a cached list in place only where the
// result is provably exact, and invalidates the lists whose membership,
// order, or total it cannot know; those refetch from page 1 when shown.
// Keyset cursors are row bounds, so inserting or removing loaded rows leaves
// every stored `nextCursor` valid.

function eachList(
  queryClient: QueryClient,
  update: (data: PostPages, params: PostListKey) => PostPages,
) {
  for (const [key, data] of queryClient.getQueriesData<PostPages>({ queryKey: postKeys.lists() })) {
    const params = postListKeyParams(key);
    if (!data || data.pages.length === 0 || !params) continue;
    const next = update(data, params);
    if (next !== data) queryClient.setQueryData<PostPages>(key, next);
  }
}

function invalidateLists(queryClient: QueryClient, match: (params: PostListKey) => boolean) {
  void queryClient.invalidateQueries({
    queryKey: postKeys.lists(),
    predicate: (query) => {
      const params = postListKeyParams(query.queryKey as QueryKey);
      return params !== null && match(params);
    },
  });
}

const isUnfilteredLatest = (params: PostListKey) =>
  params.sort === "latest" && params.query === null;

function containsPost(data: PostPages, postId: number) {
  return data.pages.some((page) => page.items.some((item) => item.id === postId));
}

// A created post is the newest readable row, so the unfiltered newest-first
// lists gain it at the top of page 1 (exact). It may or may not match a
// search, and its place in likes order depends on counts: those lists refetch.
export function addCreatedPost(queryClient: QueryClient, post: Post) {
  eachList(queryClient, (data, params) => {
    if (!isUnfilteredLatest(params) || containsPost(data, post.id)) return data;
    const total = data.pages[0].total + 1;
    return {
      ...data,
      pages: data.pages.map((page, index) => ({
        ...page,
        items: index === 0 ? [post, ...page.items] : page.items,
        total,
      })),
    };
  });
  invalidateLists(queryClient, (params) => !isUnfilteredLatest(params));
}

// An edit never changes created_at or like counts, so ordering is unchanged;
// loaded copies take the new title/content everywhere. Search membership can
// change with the text, so search lists refetch.
export function replacePost(queryClient: QueryClient, post: Post) {
  eachList(queryClient, (data) =>
    containsPost(data, post.id)
      ? {
          ...data,
          pages: data.pages.map((page) => ({
            ...page,
            items: page.items.map((item) => (item.id === post.id ? post : item)),
          })),
        }
      : data,
  );
  invalidateLists(queryClient, (params) => params.query !== null);
}

// Called once per server-confirmed delete (204). A deleted post belongs to no
// list, so its loaded rows are removed everywhere. `total` drops by one only
// where membership is certain: unfiltered lists (every readable post) and any
// list that had the row loaded. Search lists refetch to settle their totals.
export function removePost(queryClient: QueryClient, postId: number) {
  eachList(queryClient, (data, params) => {
    const loaded = containsPost(data, postId);
    if (!loaded && params.query !== null) return data;
    const total = data.pages[0].total - 1;
    return {
      ...data,
      pages: data.pages.map((page) => ({
        ...page,
        items: page.items.filter((item) => item.id !== postId),
        total,
      })),
    };
  });
  invalidateLists(queryClient, (params) => params.query !== null);
}

// A like changes the post's count and the requester's state in every loaded
// copy. Search membership and newest-first order are unaffected, but a
// likes-ordered list may now be out of order, so those lists refetch.
export function setPostLike(
  queryClient: QueryClient,
  postId: number,
  like: { isLiked: boolean; likeCount: number },
) {
  eachList(queryClient, (data) =>
    containsPost(data, postId)
      ? {
          ...data,
          pages: data.pages.map((page) => ({
            ...page,
            items: page.items.map((item) => (item.id === postId ? { ...item, ...like } : item)),
          })),
        }
      : data,
  );
  invalidateLists(queryClient, (params) => params.sort === "likes");
}

// Comment counts affect neither ordering nor search membership: loaded copies
// are updated in place and nothing refetches.
export function setPostCommentCountInLists(queryClient: QueryClient, postId: number, count: number) {
  eachList(queryClient, (data) =>
    containsPost(data, postId)
      ? {
          ...data,
          pages: data.pages.map((page) => ({
            ...page,
            items: page.items.map((item) => (item.id === postId ? { ...item, commentCount: count } : item)),
          })),
        }
      : data,
  );
}

import type { InfiniteData, QueryClient } from "@tanstack/react-query";
import type { PageResponse, Post } from "@/lib/api/types";
import { postKeys } from "./keys";

type PostPages = InfiniteData<PageResponse<Post>>;

// Edits every cached post-list infinite query. Keyset cursors are
// (createdAt, id) bounds, so adding or removing rows in cached pages leaves
// every stored `nextCursor` valid: no row is skipped or repeated.
function updateLists(queryClient: QueryClient, update: (data: PostPages) => PostPages) {
  queryClient.setQueriesData<PostPages>({ queryKey: postKeys.lists() }, (data) =>
    data && data.pages.length > 0 ? update(data) : data,
  );
}

// A created post is the newest row: prepend it to the first page.
export function addCreatedPost(queryClient: QueryClient, post: Post) {
  updateLists(queryClient, (data) => {
    if (data.pages.some((page) => page.items.some((item) => item.id === post.id))) {
      return data;
    }
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
}

export function replacePost(queryClient: QueryClient, post: Post) {
  updateLists(queryClient, (data) => ({
    ...data,
    pages: data.pages.map((page) => ({
      ...page,
      items: page.items.map((item) => (item.id === post.id ? post : item)),
    })),
  }));
}

export function removePost(queryClient: QueryClient, postId: number) {
  updateLists(queryClient, (data) => {
    if (!data.pages.some((page) => page.items.some((item) => item.id === postId))) {
      return data;
    }
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
}

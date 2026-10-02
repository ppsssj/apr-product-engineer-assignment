import { QueryClient, type InfiniteData } from "@tanstack/react-query";
import { beforeEach, describe, expect, it } from "vitest";
import type { PageResponse, Post } from "../api/types";
import { postKeys, type PostListKeyParams } from "./keys";
import {
  addCreatedPost,
  removePost,
  replacePost,
  setPostCommentCountInLists,
  setPostLike,
} from "./postListCache";

type PostPages = InfiniteData<PageResponse<Post>>;

const LATEST = { limit: 15, sort: "latest", query: null } as const;
const LIKES = { limit: 15, sort: "likes", query: null } as const;
const LATEST_SEARCH = { limit: 15, sort: "latest", query: "audit" } as const;
const LIKES_SEARCH = { limit: 15, sort: "likes", query: "audit" } as const;
const ALL_LISTS = [LATEST, LIKES, LATEST_SEARCH, LIKES_SEARCH];

function post(id: number, overrides: Partial<Post> = {}): Post {
  return {
    id,
    title: `title ${id}`,
    content: `content ${id}`,
    likeCount: 0,
    commentCount: 0,
    isLiked: false,
    author: "apr_tester",
    createdAt: "2026-09-01T00:00:00Z",
    ...overrides,
  };
}

// Two loaded pages sharing one total; the second page ends the list.
function pages(ids: number[][], total: number): PostPages {
  return {
    pages: ids.map((pageIds, index) => ({
      items: pageIds.map((id) => post(id)),
      total,
      nextCursor: index < ids.length - 1 ? `cursor-${index}` : null,
    })),
    pageParams: ids.map((_, index) => (index === 0 ? undefined : `cursor-${index - 1}`)),
  };
}

let queryClient: QueryClient;

function seed(params: PostListKeyParams, data: PostPages) {
  queryClient.setQueryData(postKeys.list(params), data);
}
function list(params: PostListKeyParams) {
  return queryClient.getQueryData<PostPages>(postKeys.list(params))!;
}
function ids(params: PostListKeyParams) {
  return list(params).pages.map((page) => page.items.map((item) => item.id));
}
function totals(params: PostListKeyParams) {
  return list(params).pages.map((page) => page.total);
}
function cursors(params: PostListKeyParams) {
  return list(params).pages.map((page) => page.nextCursor);
}
function invalidated(params: PostListKeyParams) {
  return queryClient.getQueryState(postKeys.list(params))?.isInvalidated;
}
function row(params: PostListKeyParams, id: number) {
  return list(params).pages.flatMap((page) => page.items).find((item) => item.id === id);
}

beforeEach(() => {
  queryClient = new QueryClient();
  seed(LATEST, pages([[5, 4], [3, 2]], 4));
  seed(LIKES, pages([[2, 4], [5, 3]], 4));
  seed(LATEST_SEARCH, pages([[4, 2]], 2));
  seed(LIKES_SEARCH, pages([[2, 4]], 2));
  queryClient.setQueryData(postKeys.detail(4), post(4));
});

describe("addCreatedPost", () => {
  it("prepends to the unfiltered latest list only and raises its total on every page", () => {
    addCreatedPost(queryClient, post(9));

    expect(ids(LATEST)).toEqual([[9, 5, 4], [3, 2]]);
    expect(totals(LATEST)).toEqual([5, 5]);
    expect(cursors(LATEST)).toEqual(["cursor-0", null]);
    expect(ids(LIKES)).toEqual([[2, 4], [5, 3]]);
    expect(ids(LATEST_SEARCH)).toEqual([[4, 2]]);
  });

  it("refetches every list except unfiltered latest", () => {
    addCreatedPost(queryClient, post(9));

    expect(invalidated(LATEST)).toBe(false);
    expect(invalidated(LIKES)).toBe(true);
    expect(invalidated(LATEST_SEARCH)).toBe(true);
    expect(invalidated(LIKES_SEARCH)).toBe(true);
  });

  it("does not duplicate a post that is already listed", () => {
    addCreatedPost(queryClient, post(9));
    addCreatedPost(queryClient, post(9));

    expect(ids(LATEST)).toEqual([[9, 5, 4], [3, 2]]);
    expect(totals(LATEST)).toEqual([5, 5]);
  });
});

describe("replacePost", () => {
  it("replaces every loaded copy in place without reordering or changing totals", () => {
    const edited = post(4, { title: "edited", content: "edited body" });
    replacePost(queryClient, edited);

    for (const params of ALL_LISTS) {
      expect(row(params, 4)).toEqual(edited);
    }
    expect(ids(LATEST)).toEqual([[5, 4], [3, 2]]);
    expect(ids(LIKES)).toEqual([[2, 4], [5, 3]]);
    expect(totals(LATEST)).toEqual([4, 4]);
  });

  it("refetches search lists only", () => {
    replacePost(queryClient, post(4, { title: "edited" }));

    expect(invalidated(LATEST)).toBe(false);
    expect(invalidated(LIKES)).toBe(false);
    expect(invalidated(LATEST_SEARCH)).toBe(true);
    expect(invalidated(LIKES_SEARCH)).toBe(true);
  });

  it("leaves a list without the post untouched", () => {
    const before = list(LATEST_SEARCH);
    replacePost(queryClient, post(3, { title: "edited" }));

    expect(list(LATEST_SEARCH)).toBe(before);
  });
});

describe("removePost", () => {
  it("removes every loaded copy and keeps the stored cursors", () => {
    removePost(queryClient, 4);

    expect(ids(LATEST)).toEqual([[5], [3, 2]]);
    expect(ids(LIKES)).toEqual([[2], [5, 3]]);
    expect(ids(LATEST_SEARCH)).toEqual([[2]]);
    expect(cursors(LATEST)).toEqual(["cursor-0", null]);
    expect(cursors(LIKES)).toEqual(["cursor-0", null]);
  });

  it("lowers totals where membership is certain", () => {
    removePost(queryClient, 4);

    expect(totals(LATEST)).toEqual([3, 3]);
    expect(totals(LIKES)).toEqual([3, 3]);
    expect(totals(LATEST_SEARCH)).toEqual([1]);
  });

  it("lowers unfiltered totals even when the row was not loaded", () => {
    seed(LATEST, pages([[5, 4]], 9));
    removePost(queryClient, 7);

    expect(ids(LATEST)).toEqual([[5, 4]]);
    expect(totals(LATEST)).toEqual([8]);
  });

  it("does not guess the total of a search list without the row, and refetches it", () => {
    const before = list(LATEST_SEARCH);
    removePost(queryClient, 3);

    expect(list(LATEST_SEARCH)).toBe(before);
    expect(invalidated(LATEST_SEARCH)).toBe(true);
    expect(invalidated(LATEST)).toBe(false);
    expect(invalidated(LIKES)).toBe(false);
  });
});

describe("setPostLike", () => {
  it("updates every loaded copy with the server values", () => {
    setPostLike(queryClient, 4, { isLiked: true, likeCount: 11 });

    for (const params of ALL_LISTS) {
      expect(row(params, 4)).toMatchObject({ isLiked: true, likeCount: 11 });
    }
    expect(row(LATEST, 5)).toMatchObject({ isLiked: false, likeCount: 0 });
  });

  it("refetches likes-ordered lists only", () => {
    setPostLike(queryClient, 4, { isLiked: true, likeCount: 11 });

    expect(invalidated(LIKES)).toBe(true);
    expect(invalidated(LIKES_SEARCH)).toBe(true);
    expect(invalidated(LATEST)).toBe(false);
    expect(invalidated(LATEST_SEARCH)).toBe(false);
  });

  it("keeps newest-first order", () => {
    setPostLike(queryClient, 2, { isLiked: true, likeCount: 99 });

    expect(ids(LATEST)).toEqual([[5, 4], [3, 2]]);
  });
});

describe("setPostCommentCountInLists", () => {
  it("writes one absolute count into every loaded copy without reordering or refetching", () => {
    setPostCommentCountInLists(queryClient, 4, 7);

    for (const params of ALL_LISTS) {
      expect(row(params, 4)?.commentCount).toBe(7);
      expect(invalidated(params)).toBe(false);
    }
    expect(ids(LIKES)).toEqual([[2, 4], [5, 3]]);
    expect(totals(LATEST)).toEqual([4, 4]);
  });
});

describe("list transitions", () => {
  it("never touch the post detail cache", () => {
    const detail = queryClient.getQueryData(postKeys.detail(4));
    addCreatedPost(queryClient, post(9));
    replacePost(queryClient, post(4, { title: "edited" }));
    setPostLike(queryClient, 4, { isLiked: true, likeCount: 3 });
    removePost(queryClient, 4);

    expect(queryClient.getQueryData(postKeys.detail(4))).toBe(detail);
  });
});

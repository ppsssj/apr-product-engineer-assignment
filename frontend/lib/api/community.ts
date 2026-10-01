import { request } from "./client";
import type {
  Comment,
  CommentCreateInput,
  LikeToggleResponse,
  PageParams,
  PageResponse,
  Post,
  PostListParams,
  PostMutationInput,
} from "./types";

export function getPosts(
  params: PostListParams = {},
  signal?: AbortSignal,
): Promise<PageResponse<Post>> {
  return request("/posts", {
    query: {
      limit: params.limit,
      cursor: params.cursor,
      sort: params.sort,
      q: params.query || undefined,
    },
    signal,
  });
}

export function getPost(postId: number, signal?: AbortSignal): Promise<Post> {
  return request(`/posts/${postId}`, { signal });
}

export function createPost(input: PostMutationInput): Promise<Post> {
  return request("/posts", { method: "POST", body: input });
}

export function updatePost(
  postId: number,
  input: PostMutationInput,
): Promise<Post> {
  return request(`/posts/${postId}`, { method: "PATCH", body: input });
}

export function deletePost(postId: number): Promise<void> {
  return request(`/posts/${postId}`, { method: "DELETE" });
}

export function togglePostLike(postId: number): Promise<LikeToggleResponse> {
  return request(`/posts/${postId}/likes/toggle`, { method: "POST" });
}

export function getComments(
  postId: number,
  params: PageParams = {},
  signal?: AbortSignal,
): Promise<PageResponse<Comment>> {
  return request(`/posts/${postId}/comments`, {
    query: { limit: params.limit, cursor: params.cursor },
    signal,
  });
}

export function createComment(
  postId: number,
  input: CommentCreateInput,
): Promise<Comment> {
  return request(`/posts/${postId}/comments`, { method: "POST", body: input });
}

export function deleteComment(commentId: number): Promise<void> {
  return request(`/comments/${commentId}`, { method: "DELETE" });
}

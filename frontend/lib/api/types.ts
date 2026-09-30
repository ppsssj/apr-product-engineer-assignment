// Transport shapes of the backend API. Field names and types mirror the
// backend response/request records exactly; timestamps stay ISO strings.

export type Post = {
  id: number;
  title: string;
  content: string;
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  author: string;
  createdAt: string;
};

export type Comment = {
  id: number;
  postId: number;
  content: string;
  author: string;
  createdAt: string;
};

export type PageResponse<T> = {
  items: T[];
  total: number;
  nextCursor: string | null;
};

export type LikeToggleResponse = {
  isLiked: boolean;
  likeCount: number;
};

export type PostMutationInput = {
  title: string;
  content: string;
};

export type CommentCreateInput = {
  content: string;
};

export type ApiErrorBody = {
  code: string;
  message: string;
};

export type PageParams = {
  limit?: number;
  cursor?: string;
};

export type PostSort = "latest" | "likes";

// `cursor` stays opaque: the backend encodes it for the selected sort.
export type PostListParams = PageParams & {
  sort?: PostSort;
  query?: string;
};

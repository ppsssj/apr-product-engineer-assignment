"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef } from "react";
import { ApiError } from "@/lib/api/client";
import { getPost, togglePostLike } from "@/lib/api/community";
import type { Post } from "@/lib/api/types";
import { postKeys } from "@/lib/query/keys";
import { setPostLike } from "@/lib/query/postListCache";

// 404 means the post is missing or soft-deleted; retrying cannot help.
function retryUnlessNotFound(failureCount: number, error: Error): boolean {
  if (error instanceof ApiError && error.status === 404) return false;
  return failureCount < 3;
}

export function usePostDetail(postId: number | null) {
  return useQuery({
    queryKey: postKeys.detail(postId ?? 0),
    queryFn: () => getPost(postId as number),
    enabled: postId !== null,
    retry: retryUnlessNotFound,
  });
}

// Non-optimistic toggle: the server response is the only source of the new
// like state, so a failed request never leaves a false success on screen.
// On success the same values are written into the detail cache and into any
// cached post-list page that contains this post; likes-ordered lists refetch.
export function useTogglePostLike(postId: number) {
  const queryClient = useQueryClient();
  // `isPending` reaches the render one scheduler tick after `mutate`, so a
  // fast double click could still send two toggles. This ref closes that gap.
  const inFlight = useRef(false);

  const mutation = useMutation({
    mutationFn: () => togglePostLike(postId),
    onSuccess: ({ isLiked, likeCount }) => {
      queryClient.setQueryData<Post>(postKeys.detail(postId), (post) =>
        post ? { ...post, isLiked, likeCount } : post,
      );
      setPostLike(queryClient, postId, { isLiked, likeCount });
    },
    onSettled: () => {
      inFlight.current = false;
    },
  });

  function toggle() {
    if (inFlight.current) return;
    inFlight.current = true;
    mutation.mutate();
  }

  return { toggle, isPending: mutation.isPending, isError: mutation.isError };
}

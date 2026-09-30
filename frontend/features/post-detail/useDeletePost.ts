"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { deletePost } from "@/lib/api/community";
import { useSingleFlight } from "@/lib/hooks/useSingleFlight";
import { removePost } from "@/lib/query/postListCache";
import { postKeys } from "@/lib/query/keys";

// Server-confirmed delete. On 204 the post leaves every cached list page;
// totals drop where membership is certain and search lists refetch
// (see removePost). The detail and comment caches are dropped by
// the caller after it has navigated away (`forgetDeletedPost`), so the
// still-mounted detail screen does not refetch a now-missing post.
export function useDeletePost(postId: number) {
  const queryClient = useQueryClient();
  const flight = useSingleFlight();
  const mutation = useMutation({
    mutationFn: () => deletePost(postId),
    onSuccess: () => removePost(queryClient, postId),
    onSettled: flight.end,
  });

  return {
    remove: async () => {
      if (!flight.begin()) return false;
      try {
        await mutation.mutateAsync();
        return true;
      } catch {
        return false;
      }
    },
    forgetDeletedPost: () => {
      queryClient.removeQueries({ queryKey: postKeys.detail(postId) });
    },
    isPending: mutation.isPending,
    isError: mutation.isError,
    reset: mutation.reset,
  };
}

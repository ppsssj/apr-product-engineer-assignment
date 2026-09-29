"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createPost, updatePost } from "@/lib/api/community";
import type { Post, PostMutationInput } from "@/lib/api/types";
import { useSingleFlight } from "@/lib/hooks/useSingleFlight";
import { addCreatedPost, replacePost } from "@/lib/query/postListCache";
import { postKeys } from "@/lib/query/keys";

type Submit = {
  // Resolves the server's post on success, null on failure or when another
  // submit is already in flight.
  submit: (input: PostMutationInput) => Promise<Post | null>;
  isPending: boolean;
  isError: boolean;
};

// Server-confirmed: caches change only after the request succeeds, using the
// complete `Post` the server returns.
export function useCreatePost(): Submit {
  const queryClient = useQueryClient();
  const flight = useSingleFlight();
  const mutation = useMutation({
    mutationFn: (input: PostMutationInput) => createPost(input),
    onSuccess: (post) => {
      queryClient.setQueryData(postKeys.detail(post.id), post);
      addCreatedPost(queryClient, post);
    },
    onSettled: flight.end,
  });
  return withFlight(mutation, flight);
}

export function useUpdatePost(postId: number): Submit {
  const queryClient = useQueryClient();
  const flight = useSingleFlight();
  const mutation = useMutation({
    mutationFn: (input: PostMutationInput) => updatePost(postId, input),
    onSuccess: (post) => {
      queryClient.setQueryData(postKeys.detail(post.id), post);
      replacePost(queryClient, post);
    },
    onSettled: flight.end,
  });
  return withFlight(mutation, flight);
}

function withFlight(
  mutation: {
    mutateAsync: (input: PostMutationInput) => Promise<Post>;
    isPending: boolean;
    isError: boolean;
  },
  flight: ReturnType<typeof useSingleFlight>,
): Submit {
  return {
    submit: async (input) => {
      if (!flight.begin()) return null;
      try {
        return await mutation.mutateAsync(input);
      } catch {
        return null;
      }
    },
    isPending: mutation.isPending,
    isError: mutation.isError,
  };
}

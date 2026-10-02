import { ApiError } from "@/lib/api/client";

// `failureCount` is zero for the first failed attempt. Returning true only at
// zero preserves one automatic recovery attempt, while keeping the library's
// default retry delay (1 s for that retry).
//
// The backend's ApiError represents a completed HTTP response. Any 4xx is a
// deterministic client/request failure for this API contract, so repeating the
// identical read cannot produce a different result. Network failures, 5xx
// responses, and unknown read failures retain one retry.
export function retryOnceUnlessDeterministicClientError(
  failureCount: number,
  error: Error,
): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }

  return failureCount < 1;
}

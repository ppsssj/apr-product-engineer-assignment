import { describe, expect, it } from "vitest";
import { ApiError } from "../api/client";
import { retryOnceUnlessDeterministicClientError as retry } from "./retryPolicy";

describe("retryOnceUnlessDeterministicClientError", () => {
  it("never retries a completed 4xx response", () => {
    for (const status of [400, 404, 429]) {
      expect(retry(0, new ApiError(status, "ANY", "client error")), String(status)).toBe(false);
    }
  });

  it("retries a 5xx, network, invalid-response, or unknown failure exactly once", () => {
    for (const error of [
      new ApiError(500, "INTERNAL_ERROR", "server error"),
      new ApiError(0, "NETWORK_ERROR", "Network request failed"),
      new ApiError(200, "INVALID_RESPONSE", "Response body is not valid JSON"),
      new Error("unknown"),
    ]) {
      expect(retry(0, error), error.message).toBe(true);
      expect(retry(1, error), error.message).toBe(false);
    }
  });
});

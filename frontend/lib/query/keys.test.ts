import { describe, expect, it } from "vitest";
import { normalizePostQuery, postKeys } from "./keys";

describe("normalizePostQuery", () => {
  it("treats a missing or blank search as no search and trims the rest", () => {
    expect(normalizePostQuery(undefined)).toBeNull();
    expect(normalizePostQuery(null)).toBeNull();
    expect(normalizePostQuery("   ")).toBeNull();
    expect(normalizePostQuery("  레이저 시술 ")).toBe("레이저 시술");
  });

  it("gives a blank search the same list key as no search", () => {
    expect(postKeys.list({ limit: 15, sort: "likes", query: "  " })).toEqual(
      postKeys.list({ limit: 15, sort: "likes" }),
    );
  });
});

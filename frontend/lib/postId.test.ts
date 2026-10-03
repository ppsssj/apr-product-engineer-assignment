import { describe, expect, it } from "vitest";
import { parsePostId } from "./postId";

describe("parsePostId", () => {
  it("accepts positive safe integers", () => {
    expect(parsePostId("1")).toBe(1);
    expect(parsePostId("42")).toBe(42);
    expect(parsePostId(String(Number.MAX_SAFE_INTEGER))).toBe(Number.MAX_SAFE_INTEGER);
  });

  it("rejects segments that are not a canonical positive safe integer", () => {
    for (const segment of [
      "0",
      "-1",
      "1.5",
      "abc",
      "001",
      "",
      " 1",
      "1e3",
      "9007199254740992", // MAX_SAFE_INTEGER + 1
      "99999999999999999999",
    ]) {
      expect(parsePostId(segment), segment).toBeNull();
    }
  });
});

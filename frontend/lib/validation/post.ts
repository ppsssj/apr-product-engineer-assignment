// Mirrors the backend MutationValidator so the form enables submit exactly
// when the server would accept the input. The submitted values are never
// trimmed or otherwise changed.

export const MAX_TITLE_LENGTH = 20;

// Java `Character.isWhitespace`: space separators except the no-break spaces
// (U+00A0, U+2007, U+202F), line/paragraph separators, and \t \n \v \f \r
// plus U+001C–U+001F. `String.isBlank()` is true when every code point is one.
const JAVA_WHITESPACE = new RegExp(
  "^[\t\n\v\f\r\u001C-\u001F \u1680\u2000-\u2006\u2008-\u200A\u2028\u2029\u205F\u3000]$",
  "u",
);

export function isBlank(value: string): boolean {
  for (const char of value) {
    if (!JAVA_WHITESPACE.test(char)) return false;
  }
  return true;
}

// Java `codePointCount`: an emoji outside the BMP counts once, not twice as
// with `String.length`.
export function codePointLength(value: string): number {
  return Array.from(value).length;
}

export type PostFormErrors = {
  title: "required" | "tooLong" | null;
  content: "required" | null;
};

export function validatePost(title: string, content: string): PostFormErrors {
  return {
    title: isBlank(title)
      ? "required"
      : codePointLength(title) > MAX_TITLE_LENGTH
        ? "tooLong"
        : null,
    content: isBlank(content) ? "required" : null,
  };
}

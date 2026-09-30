// The comment and heart paths are normalized from 755-8831-post-list.svg.
// They inherit color so the list can distinguish neutral and requester-liked
// hearts without changing count text. The pencil remains a local approximation.

type IconProps = { size?: number };

// Toolbar glyphs (PRODUCT_DECISION; not in the official frames). Stroke icons
// that inherit color like the rest of the list.
export function SearchIcon({ size = 18 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 18 18" fill="none" aria-hidden="true" focusable="false">
      <circle cx="8" cy="8" r="5.5" stroke="currentColor" strokeWidth="1.6" />
      <path d="m12.2 12.2 3.6 3.6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

export function SearchBackIcon({ size = 24 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
      <path
        d="M20 12H4.5M10.5 5.5 4 12l6.5 6.5"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function ClearIcon({ size = 18 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 18 18" aria-hidden="true" focusable="false">
      <circle cx="9" cy="9" r="8" fill="currentColor" />
      <path d="m6.2 6.2 5.6 5.6m0-5.6-5.6 5.6" stroke="#ffffff" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export function CommentIcon({ size = 14 }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 14 14"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M0 4.912C0 2.199 2.1993 0 4.9124 0H9.0876C11.8007 0 14 2.199 14 4.912C14 7.211 12.4211 9.14 10.2885 9.676V11.349C10.2885 11.885 9.6775 12.19 9.2494 11.869L6.5223 9.824H4.9124C2.1993 9.824 0 7.625 0 4.912Z"
        fill="currentColor"
        transform="translate(0 1)"
      />
    </svg>
  );
}

export function LikeIcon({ size = 14 }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 14 14"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M11.0747.884C10.7815.604 10.4333.381 10.0502.23C9.667.078 9.2563 0 8.8415 0C8.4268 0 8.0161.078 7.6329.23C7.2497.381 6.9016.604 6.6084.884L5.9998 1.465L5.3913.884C4.7991.318 3.9958 0 3.1582 0C2.3206 0 1.5173.318.925.884C.3327 1.45 0 2.217 0 3.018C0 3.818.3327 4.585.925 5.151L1.5335 5.733L5.9998 10L10.4662 5.733L11.0747 5.151C11.368 4.871 11.6007 4.539 11.7595 4.172C11.9183 3.806 12 3.414 12 3.018C12 2.621 11.9183 2.229 11.7595 1.863C11.6007 1.497 11.368 1.164 11.0747.884Z"
        fill="currentColor"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
        transform="translate(1 2)"
      />
    </svg>
  );
}

export function PencilIcon({ size = 24 }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M14.5 5.5l4 4M4.5 19.5l1-4.5L15.8 4.7a1.4 1.4 0 0 1 2 0l1.5 1.5a1.4 1.4 0 0 1 0 2L9 18.5l-4.5 1Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

// Inline icons drawn for the list. Figma shows comment/like/pencil glyphs, but
// their exact paths and sizes were not extracted (docs/UI_SPEC.md §6.4), so
// these are PRODUCT_DECISION approximations that inherit `currentColor`.

type IconProps = { size?: number };

export function CommentIcon({ size = 14 }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M8 2.25c3.31 0 5.75 2.1 5.75 4.75S11.31 11.75 8 11.75c-.6 0-1.18-.07-1.72-.2L3.25 13.5l.68-2.92C2.9 9.72 2.25 8.44 2.25 7 2.25 4.35 4.69 2.25 8 2.25Z"
        stroke="currentColor"
        strokeWidth="1.3"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function LikeIcon({ size = 14 }: IconProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M8 13.5S2.25 10.1 2.25 6.1A3.1 3.1 0 0 1 8 4.4a3.1 3.1 0 0 1 5.75 1.7c0 4-5.75 7.4-5.75 7.4Z"
        stroke="currentColor"
        strokeWidth="1.3"
        strokeLinejoin="round"
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

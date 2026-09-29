// Inline detail icons. Their shapes follow the local Figma PNG exports, but
// the Figma paths were not extracted, so they are PRODUCT_DECISION drawings.

export function BackIcon() {
  return (
    <svg
      width={24}
      height={24}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
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

export function HeartIcon({ filled }: { filled: boolean }) {
  return (
    <svg
      width={24}
      height={24}
      viewBox="0 0 24 24"
      fill={filled ? "currentColor" : "none"}
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M12 20s-8-4.9-8-10.6A4.4 4.4 0 0 1 12 6.8a4.4 4.4 0 0 1 8 2.6C20 15.1 12 20 12 20Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function ChevronDownIcon() {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="m3.5 6 4.5 4.5L12.5 6"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function MoreIcon() {
  return (
    <svg width={24} height={24} viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="5" r="1.7" fill="currentColor" />
      <circle cx="12" cy="12" r="1.7" fill="currentColor" />
      <circle cx="12" cy="19" r="1.7" fill="currentColor" />
    </svg>
  );
}

export function EditIcon() {
  return (
    <svg width={24} height={24} viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
      <path
        d="M14.5 5.5l4 4M4.5 19.5l1-4.5L15.8 4.7a1.4 1.4 0 0 1 2 0l1.5 1.5a1.4 1.4 0 0 1 0 2L9 18.5l-4.5 1Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function TrashIcon() {
  return (
    <svg width={24} height={24} viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
      <path
        d="M4.5 6.5h15M9.5 6.5V4.8c0-.4.3-.8.8-.8h3.4c.5 0 .8.4.8.8v1.7M6.5 6.5l.8 12.2c.1.7.6 1.3 1.4 1.3h6.6c.8 0 1.3-.6 1.4-1.3l.8-12.2M10.3 10.5v6M13.7 10.5v6"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

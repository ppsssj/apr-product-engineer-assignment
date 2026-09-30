"use client";

import { useEffect, useRef, type RefObject } from "react";
import styles from "./CommentDeletePopover.module.css";

type Props = {
  id: string;
  // The comment's own "삭제" button; Escape returns focus to it.
  triggerRef: RefObject<HTMLButtonElement | null>;
  pending: boolean;
  error: string | null;
  onConfirm: () => void;
  onClose: () => void;
};

// EXPERIMENTAL P8-C PRODUCT_DECISION: a contextual, non-modal confirmation
// beside the comment's "삭제" (post deletion keeps its modal dialog). No scrim;
// a press outside it or its trigger, or Escape, closes it, except while the
// DELETE is in flight. On failure it stays open with the error for a retry.
export function CommentDeletePopover({ id, triggerRef, pending, error, onConfirm, onClose }: Props) {
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onPointerDown(event: PointerEvent) {
      if (pending) return;
      const target = event.target as Node;
      // The trigger toggles on its own click, so it is not "outside".
      if (rootRef.current?.contains(target) || triggerRef.current?.contains(target)) return;
      onClose();
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape" || pending) return;
      onClose();
      triggerRef.current?.focus();
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [pending, onClose, triggerRef]);

  return (
    <div ref={rootRef} id={id} className={styles.popover} data-error={error !== null}>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <button type="button" className={styles.confirm} disabled={pending} onClick={onConfirm}>
        삭제하기
      </button>
    </div>
  );
}

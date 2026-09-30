"use client";

import { useEffect, useRef, type RefObject } from "react";
import { EditIcon, TrashIcon } from "./icons";
import styles from "./PostActionMenu.module.css";

type Props = {
  id: string;
  // The `⋮` button; Escape returns focus to it.
  triggerRef: RefObject<HTMLButtonElement | null>;
  onEdit: () => void;
  onDelete: () => void;
  onClose: () => void;
};

// Own-post actions as a non-modal popover anchored under the post's `⋮`
// (P8-C PRODUCT_DECISION; the official design uses a bottom sheet). Two
// plain buttons, no scrim and no cancel row: a press outside the menu or its
// trigger, or Escape, closes it. Rendered inside the trigger's positioned
// wrapper, so it overlays the post without moving it.
export function PostActionMenu({ id, triggerRef, onEdit, onDelete, onClose }: Props) {
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onPointerDown(event: PointerEvent) {
      const target = event.target as Node;
      // The trigger toggles on its own click, so it is not "outside".
      if (rootRef.current?.contains(target) || triggerRef.current?.contains(target)) return;
      onClose();
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape") return;
      onClose();
      triggerRef.current?.focus();
    }
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [onClose, triggerRef]);

  return (
    <div ref={rootRef} id={id} className={styles.menu}>
      <button type="button" className={styles.item} onClick={onEdit}>
        <EditIcon />
        수정하기
      </button>
      <button type="button" className={styles.item} onClick={onDelete}>
        <TrashIcon />
        삭제하기
      </button>
    </div>
  );
}

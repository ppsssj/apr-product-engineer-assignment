"use client";

import { useEffect, useRef } from "react";
import { EditIcon, TrashIcon } from "./icons";
import styles from "./PostActionSheet.module.css";

type Props = {
  onEdit: () => void;
  onDelete: () => void;
  onClose: () => void;
};

// Own-post action sheet (SCREEN-005): edit, delete, cancel over a full-screen
// scrim. Focus moves to the first action on open and back to the trigger on
// close; Escape and the scrim close it.
export function PostActionSheet({ onEdit, onDelete, onClose }: Props) {
  const firstRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    firstRef.current?.focus();
    return () => opener?.focus();
  }, []);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div
      className={styles.scrim}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div role="dialog" aria-modal="true" aria-label="게시글 메뉴" className={styles.sheet}>
        <button ref={firstRef} type="button" className={styles.item} onClick={onEdit}>
          <EditIcon />
          수정하기
        </button>
        <hr className={styles.divider} />
        <button type="button" className={styles.item} onClick={onDelete}>
          <TrashIcon />
          삭제하기
        </button>
        <button type="button" className={styles.cancel} onClick={onClose}>
          취소
        </button>
      </div>
    </div>
  );
}

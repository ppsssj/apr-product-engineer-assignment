"use client";

import { useEffect, useId, useRef } from "react";
import styles from "./ConfirmDialog.module.css";

type Props = {
  title: string;
  body: string;
  pending: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
};

// Centered confirmation over a full-screen scrim (docs/UI_SPEC.md "Delete
// Confirmations"). Focus moves to Cancel on open and returns to the opener on
// close; Escape and the scrim cancel unless a request is in flight. Tab and
// Shift+Tab cycle between the dialog's buttons and never reach the page behind.
export function ConfirmDialog({ title, body, pending, error, onCancel, onConfirm }: Props) {
  const titleId = useId();
  const bodyId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    cancelRef.current?.focus();
    return () => opener?.focus();
  }, []);

  // Disabling the focused button while pending drops focus to <body>. Once the
  // request settles with the dialog still open (a failed delete), bring focus
  // back. Runs after the open effect above, so on mount Cancel already has it.
  useEffect(() => {
    if (pending) return;
    if (!dialogRef.current?.contains(document.activeElement)) {
      cancelRef.current?.focus();
    }
  }, [pending]);

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !pending) onCancel();
      if (event.key !== "Tab" || !dialogRef.current) return;

      // The only controls are the two action buttons; both are disabled while
      // pending, and then Tab must not move focus anywhere.
      const buttons = Array.from(
        dialogRef.current.querySelectorAll<HTMLButtonElement>("button:enabled"),
      );
      const first = buttons[0];
      const last = buttons[buttons.length - 1];
      const active = document.activeElement;
      if (!first || !last) {
        event.preventDefault();
      } else if (!dialogRef.current.contains(active)) {
        event.preventDefault();
        (event.shiftKey ? last : first).focus();
      } else if (event.shiftKey && active === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [pending, onCancel]);

  return (
    <div
      className={styles.scrim}
      onClick={(event) => {
        if (event.target === event.currentTarget && !pending) onCancel();
      }}
    >
      <div
        ref={dialogRef}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={bodyId}
        className={styles.dialog}
      >
        <h2 id={titleId} className={styles.title}>
          {title}
        </h2>
        <p id={bodyId} className={styles.body}>
          {body}
        </p>
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
        <div className={styles.actions}>
          <button
            ref={cancelRef}
            type="button"
            className={styles.cancel}
            onClick={onCancel}
            disabled={pending}
          >
            취소
          </button>
          <button
            type="button"
            className={styles.confirm}
            onClick={onConfirm}
            disabled={pending}
          >
            예
          </button>
        </div>
      </div>
    </div>
  );
}

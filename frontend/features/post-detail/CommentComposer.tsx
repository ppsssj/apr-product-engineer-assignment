"use client";

import { useState, type FormEvent, type KeyboardEvent } from "react";
import { isBlank } from "@/lib/validation/post";
import { useCreateComment } from "./useCommentMutations";
import styles from "./CommentComposer.module.css";

export function CommentComposer({ postId }: { postId: number }) {
  const [content, setContent] = useState("");
  const create = useCreateComment(postId);
  // Same rule as the backend (`isBlank`): whitespace-only cannot be sent. The
  // content itself is sent unchanged.
  const canSubmit = !isBlank(content) && !create.isPending;

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isBlank(content)) return;
    // Cleared only after the server confirms; on failure the text stays.
    if (await create.submit(content)) setContent("");
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // Enter that commits Korean IME composition must not submit the form.
    if (event.key === "Enter" && event.nativeEvent.isComposing) {
      event.preventDefault();
    }
  }

  return (
    <form className={styles.composer} onSubmit={onSubmit}>
      {create.isError && (
        <p className={styles.error} role="alert">
          댓글을 등록하지 못했습니다. 다시 시도해 주세요.
        </p>
      )}
      <div className={styles.field}>
        <input
          className={styles.input}
          value={content}
          onChange={(event) => {
            setContent(event.target.value);
            if (create.isError) create.reset();
          }}
          onKeyDown={onKeyDown}
          placeholder="댓글을 입력하세요."
          aria-label="댓글 입력"
          readOnly={create.isPending}
        />
        <button type="submit" className={styles.submit} disabled={!canSubmit}>
          등록
        </button>
      </div>
    </form>
  );
}

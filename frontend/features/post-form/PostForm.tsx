"use client";

import { useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import type { PostMutationInput } from "@/lib/api/types";
import { MAX_TITLE_LENGTH, validatePost } from "@/lib/validation/post";
import styles from "./PostForm.module.css";

type Props = {
  heading: string;
  submitLabel: string;
  initialTitle?: string;
  initialContent?: string;
  pending: boolean;
  errorMessage: string | null;
  // Resolves true once the server accepted the input; the caller navigates.
  onSubmit: (input: PostMutationInput) => Promise<boolean>;
};

const CONTENT_PLACEHOLDER =
  "게시글 내용을 작성해주세요.\n부적합한 내용은 삭제될 수 있으며, 비방/욕설 등의 내용으로 신고가 접수될 경우 서비스 이용이 제한될 수 있습니다.";

// Shared create/edit form (SCREEN-003/004): same layout and validation; only
// the copy, the initial values, and the submit action differ.
export function PostForm({
  heading,
  submitLabel,
  initialTitle = "",
  initialContent = "",
  pending,
  errorMessage,
  onSubmit,
}: Props) {
  const [title, setTitle] = useState(initialTitle);
  const [content, setContent] = useState(initialContent);
  const contentRef = useRef<HTMLTextAreaElement>(null);
  const errors = validatePost(title, content);
  const valid = errors.title === null && errors.content === null;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!valid) return;
    // Values are sent exactly as typed and stay in the form if the request fails.
    await onSubmit({ title, content });
  }

  function onTitleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // Enter in the title moves to the content instead of submitting the form
    // (and never while a Korean IME composition is being committed).
    if (event.key !== "Enter") return;
    event.preventDefault();
    if (!event.nativeEvent.isComposing) contentRef.current?.focus();
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      <h1 className={styles.heading}>{heading}</h1>
      <input
        className={styles.title}
        data-filled={title.length > 0}
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        onKeyDown={onTitleKeyDown}
        placeholder={`제목을 입력해 주세요. (최대 ${MAX_TITLE_LENGTH}자)`}
        aria-label="제목"
        aria-invalid={errors.title === "tooLong"}
        aria-describedby={errors.title === "tooLong" ? "title-hint" : undefined}
        readOnly={pending}
      />
      {errors.title === "tooLong" && (
        <p id="title-hint" className={styles.hint} role="alert">
          제목은 최대 {MAX_TITLE_LENGTH}자까지 입력할 수 있습니다.
        </p>
      )}
      <textarea
        ref={contentRef}
        className={styles.content}
        value={content}
        onChange={(event) => setContent(event.target.value)}
        placeholder={CONTENT_PLACEHOLDER}
        aria-label="내용"
        readOnly={pending}
      />
      <div className={styles.actionBar}>
        {errorMessage && (
          <p className={styles.error} role="alert">
            {errorMessage}
          </p>
        )}
        <button type="submit" className={styles.submit} disabled={!valid || pending}>
          {submitLabel}
        </button>
      </div>
    </form>
  );
}

"use client";

import { useEffect, useRef, type KeyboardEvent } from "react";
import type { PostSort } from "@/lib/api/types";
import { ClearIcon, SearchBackIcon, SearchIcon } from "./icons";
import styles from "./PostListToolbar.module.css";

type Props = {
  sort: PostSort;
  onSortChange: (sort: PostSort) => void;
  searchOpen: boolean;
  searchInput: string;
  onSearchInputChange: (value: string) => void;
  onOpenSearch: () => void;
  onCloseSearch: () => void;
  onClearSearch: () => void;
  onSubmitSearch: () => void;
};

const SORT_OPTIONS: { value: PostSort; label: string }[] = [
  { value: "latest", label: "최신순" },
  { value: "likes", label: "좋아요순" },
];

// Discovery row under the app title (PRODUCT_DECISION). Normal mode: a search
// action on the left and the two sort choices on the right. Search mode swaps
// the same 44px row for a query field; the chosen sort still applies.
export function PostListToolbar({
  sort,
  onSortChange,
  searchOpen,
  searchInput,
  onSearchInputChange,
  onOpenSearch,
  onCloseSearch,
  onClearSearch,
  onSubmitSearch,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const wasOpen = useRef(searchOpen);

  // Opening focuses the field; closing hands focus back to the trigger.
  useEffect(() => {
    if (searchOpen) inputRef.current?.focus();
    else if (wasOpen.current) triggerRef.current?.focus();
    wasOpen.current = searchOpen;
  }, [searchOpen]);

  // Handled on the whole search row, so Escape works from the field, ← or ×.
  function onKeyDown(event: KeyboardEvent<HTMLFormElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      onCloseSearch();
    }
  }

  if (searchOpen) {
    return (
      <form
        role="search"
        className={styles.toolbar}
        onKeyDown={onKeyDown}
        onSubmit={(event) => {
          event.preventDefault();
          onSubmitSearch();
        }}
      >
        <button type="button" className={styles.back} onClick={onCloseSearch} aria-label="검색 닫기">
          <SearchBackIcon />
        </button>
        <div className={styles.field}>
          <input
            ref={inputRef}
            className={styles.input}
            value={searchInput}
            onChange={(event) => onSearchInputChange(event.target.value)}
            placeholder="게시글 검색"
            aria-label="게시글 검색"
            enterKeyHint="search"
            autoComplete="off"
            maxLength={100}
          />
          {searchInput.length > 0 && (
            <button
              type="button"
              className={styles.clear}
              onClick={() => {
                onClearSearch();
                // The × disappears with the text; keep focus in the field.
                inputRef.current?.focus();
              }}
              aria-label="검색어 지우기"
            >
              <ClearIcon />
            </button>
          )}
        </div>
      </form>
    );
  }

  // Sorting (the list's persistent state) comes first; the search action
  // follows as one button that fills the rest of the row, so the whole blank
  // area after "검색" is its hit target. DOM order = visual = tab order.
  return (
    <div className={styles.toolbar}>
      <div className={styles.sorts} role="group" aria-label="정렬">
        {SORT_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            className={styles.sort}
            aria-pressed={sort === option.value}
            onClick={() => onSortChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
      <button
        ref={triggerRef}
        type="button"
        className={styles.searchTrigger}
        onClick={onOpenSearch}
        aria-label="게시글 검색"
      >
        <span className={styles.searchContent}>
          <SearchIcon />
          검색
        </span>
      </button>
    </div>
  );
}

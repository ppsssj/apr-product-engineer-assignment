"use client";

import { useEffect, useRef, type KeyboardEvent } from "react";
import type { PostSort } from "@/lib/api/types";
import { SearchBackIcon, SearchIcon } from "./icons";
import styles from "./PostListToolbar.module.css";

type Props = {
  sort: PostSort;
  onSortChange: (sort: PostSort) => void;
  searchOpen: boolean;
  searchInput: string;
  onSearchInputChange: (value: string) => void;
  onOpenSearch: () => void;
  // Closing clears the query and keeps the sort (PostListScreen.closeSearch).
  onCloseSearch: () => void;
  onSubmitSearch: () => void;
};

const SORT_OPTIONS: { value: PostSort; label: string }[] = [
  { value: "latest", label: "최신순" },
  { value: "likes", label: "좋아요순" },
];

// Discovery row under the app title (PRODUCT_DECISION). Normal mode: the two
// sort choices, then a wide search action. Search mode keeps the same flat
// 44px row: ←, a decorative search icon, and the query field. ← (or Escape)
// is the only exit; it clears the query and keeps the chosen sort.
export function PostListToolbar({
  sort,
  onSortChange,
  searchOpen,
  searchInput,
  onSearchInputChange,
  onOpenSearch,
  onCloseSearch,
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

  // Handled on the search row, so Escape closes search whether focus is in the field or on ←.
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
        className={`${styles.toolbar} ${styles.searchMode}`}
        onKeyDown={onKeyDown}
        onSubmit={(event) => {
          event.preventDefault();
          onSubmitSearch();
        }}
      >
        <button type="button" className={styles.back} onClick={onCloseSearch} aria-label="검색 닫기">
          <SearchBackIcon />
        </button>
        {/* Decorative: the input carries the accessible name. */}
        <span className={styles.searchModeIcon}>
          <SearchIcon />
        </span>
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

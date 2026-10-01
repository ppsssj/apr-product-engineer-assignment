"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import type { PostSort } from "@/lib/api/types";
import { takeFlash } from "@/lib/flash";
import { rememberFormOrigin } from "@/lib/formOrigin";
import { useInfiniteScrollTrigger } from "@/lib/hooks/useInfiniteScrollTrigger";
import { readPostListView, rememberPostListView } from "@/lib/postListView";
import { normalizePostQuery } from "@/lib/query/keys";
import { routes } from "@/lib/routes";
import { PencilIcon } from "./icons";
import { PostListItem } from "./PostListItem";
import { PostListToolbar } from "./PostListToolbar";
import { usePostList } from "./usePostList";
import styles from "./PostListScreen.module.css";

const TOAST_DURATION_MS = 2500;
const SEARCH_DEBOUNCE_MS = 300;

export function PostListScreen() {
  // Discovery state. `searchInput` follows every keystroke; `query` is the
  // committed (debounced) search that the list request uses. The sort is kept
  // while searching and after the search closes. The last view is remembered
  // for in-app back navigation (lib/postListView.ts).
  const [initialView] = useState(readPostListView);
  const [sort, setSort] = useState<PostSort>(initialView.sort);
  const [searchOpen, setSearchOpen] = useState(initialView.searchOpen);
  const [searchInput, setSearchInput] = useState(initialView.query ?? "");
  const [query, setQuery] = useState<string | null>(initialView.query);

  useEffect(() => {
    rememberPostListView({ sort, searchOpen, query });
  }, [sort, searchOpen, query]);

  useEffect(() => {
    const next = normalizePostQuery(searchInput);
    if (next === query) return;
    const timer = window.setTimeout(() => setQuery(next), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [searchInput, query]);

  // A new sort or search is a different list from its first page. If the
  // toolbar is stuck (the title scrolled away), bring the new results' top
  // back under it instead of leaving the viewport mid-list.
  const headerRef = useRef<HTMLElement>(null);
  const firstView = useRef(true);
  useEffect(() => {
    if (firstView.current) {
      firstView.current = false;
      return;
    }
    const headerHeight = headerRef.current?.offsetHeight ?? 0;
    if (window.scrollY > headerHeight) window.scrollTo({ top: headerHeight });
  }, [sort, query]);

  const {
    data,
    status,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isFetchNextPageError,
  } = usePostList(sort, query);

  const sentinelRef = useInfiniteScrollTrigger<HTMLDivElement>({
    enabled:
      data !== undefined &&
      hasNextPage &&
      !isFetchingNextPage &&
      !isFetchNextPageError,
    onIntersect: () => void fetchNextPage(),
  });

  // One-shot message from the previous screen (the create toast).
  const [toast, setToast] = useState<string | null>(null);
  useEffect(() => {
    const message = takeFlash();
    if (message) setToast(message);
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), TOAST_DURATION_MS);
    return () => window.clearTimeout(timer);
  }, [toast]);

  // Pages keep their server order. Like counts can change between page
  // requests of a likes-ordered list, so a post could reappear on a later
  // page; only its first occurrence is rendered.
  const posts = useMemo(() => {
    const seen = new Set<number>();
    return (data?.pages ?? [])
      .flatMap((page) => page.items)
      .filter((post) => (seen.has(post.id) ? false : (seen.add(post.id), true)));
  }, [data]);
  // Data is fetched in the browser only, so the clock is read after hydration.
  const now = Date.now();

  function closeSearch() {
    setSearchOpen(false);
    setSearchInput("");
    setQuery(null);
  }

  function scrollToTop() {
    const behavior = window.matchMedia("(prefers-reduced-motion: reduce)").matches
      ? "auto"
      : "smooth";
    window.scrollTo({ top: 0, behavior });
  }

  const canScrollToTop =
    posts.length > 0 &&
    hasNextPage === false &&
    isFetchingNextPage === false &&
    isFetchNextPageError === false;

  return (
    <div className={styles.screen}>
      <header ref={headerRef} className={styles.header}>
        <h1 className={styles.appTitle}>메디큐브톡</h1>
      </header>

      <PostListToolbar
        sort={sort}
        onSortChange={setSort}
        searchOpen={searchOpen}
        searchInput={searchInput}
        onSearchInputChange={setSearchInput}
        onOpenSearch={() => setSearchOpen(true)}
        onCloseSearch={closeSearch}
        onSubmitSearch={() => setQuery(normalizePostQuery(searchInput))}
      />

      <main className={styles.main}>
        {status === "pending" && (
          <p className={styles.notice} role="status">
            게시글을 불러오는 중입니다.
          </p>
        )}

        {/* A failed next page also sets status to "error"; loaded pages stay
            visible and that failure is reported below the list instead. */}
        {status === "error" && data === undefined && (
          <div className={styles.notice} role="alert">
            <p className={styles.noticeText}>게시글을 불러오지 못했습니다.</p>
            <button
              type="button"
              className={styles.retry}
              onClick={() => void refetch()}
            >
              다시 시도
            </button>
          </div>
        )}

        {data !== undefined && posts.length === 0 && (
          <p className={styles.notice}>
            {query ? "검색 결과가 없습니다." : "아직 작성된 게시글이 없습니다."}
          </p>
        )}

        {posts.length > 0 && (
          <ul className={styles.list}>
            {posts.map((post) => (
              <PostListItem key={post.id} post={post} now={now} />
            ))}
            {canScrollToTop && (
              <li className={styles.backToTopItem}>
                <button type="button" className={styles.backToTop} onClick={scrollToTop}>
                  위로 가기
                </button>
              </li>
            )}
          </ul>
        )}

        {isFetchingNextPage && (
          <p className={styles.pageStatus} role="status">
            게시글을 더 불러오는 중입니다.
          </p>
        )}

        {isFetchNextPageError && (
          <div className={styles.pageStatus} role="alert">
            <p className={styles.noticeText}>
              다음 게시글을 불러오지 못했습니다.
            </p>
            <button
              type="button"
              className={styles.retry}
              onClick={() => void fetchNextPage()}
            >
              다시 시도
            </button>
          </div>
        )}

        <div ref={sentinelRef} className={styles.sentinel} aria-hidden="true" />
      </main>

      <Link
        href={routes.postCreate()}
        className={styles.fab}
        aria-label="게시글 작성"
        onClick={() => rememberFormOrigin(routes.home())}
      >
        <PencilIcon />
      </Link>

      {toast && (
        <p className={styles.toast} role="status">
          {toast}
        </p>
      )}
    </div>
  );
}

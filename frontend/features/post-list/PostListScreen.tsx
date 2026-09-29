"use client";

import Link from "next/link";
import { useInfiniteScrollTrigger } from "@/lib/hooks/useInfiniteScrollTrigger";
import { routes } from "@/lib/routes";
import { PencilIcon } from "./icons";
import { PostListItem } from "./PostListItem";
import { usePostList } from "./usePostList";
import styles from "./PostListScreen.module.css";

export function PostListScreen() {
  const {
    data,
    status,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isFetchNextPageError,
  } = usePostList();

  const sentinelRef = useInfiniteScrollTrigger<HTMLDivElement>({
    enabled:
      data !== undefined &&
      hasNextPage &&
      !isFetchingNextPage &&
      !isFetchNextPageError,
    onIntersect: () => void fetchNextPage(),
  });

  const posts = data?.pages.flatMap((page) => page.items) ?? [];
  // Data is fetched in the browser only, so the clock is read after hydration.
  const now = Date.now();

  return (
    <div className={styles.screen}>
      <header className={styles.header}>
        <h1 className={styles.appTitle}>메디큐브톡</h1>
      </header>

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
          <p className={styles.notice}>아직 작성된 게시글이 없습니다.</p>
        )}

        {posts.length > 0 && (
          <ul className={styles.list}>
            {posts.map((post) => (
              <PostListItem key={post.id} post={post} now={now} />
            ))}
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
      >
        <PencilIcon />
      </Link>
    </div>
  );
}

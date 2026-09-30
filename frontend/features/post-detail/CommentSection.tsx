"use client";

import { useState } from "react";
import { avatarFor } from "@/lib/avatar";
import { useInfiniteScrollTrigger } from "@/lib/hooks/useInfiniteScrollTrigger";
import { readCurrentRequester } from "@/lib/requester";
import { formatRelativeTime } from "@/lib/time/relativeTime";
import { CommentComposer } from "./CommentComposer";
import { ConfirmDialog } from "./ConfirmDialog";
import { ChevronDownIcon } from "./icons";
import { useDeleteComment } from "./useCommentMutations";
import { INITIAL_COMMENT_COUNT, useComments } from "./useComments";
import styles from "./CommentSection.module.css";

type Props = {
  postId: number;
  commentCount: number;
};

export function CommentSection({ postId, commentCount }: Props) {
  // "More" is per visit. The comment query is not kept after unmount
  // (useComments: gcTime 0), so every visit starts from the initial page.
  const [expanded, setExpanded] = useState(false);
  const {
    data,
    status,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isFetchNextPageError,
  } = useComments(postId);
  // Comments render only after a browser fetch, so reading the cookie here
  // cannot differ from a server render. /dev-user switches users with a full
  // page load, which re-runs this.
  const [requester] = useState(readCurrentRequester);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);
  const removal = useDeleteComment(postId);

  function closeDialog() {
    setDeleteTarget(null);
    removal.reset();
  }

  async function confirmDelete() {
    if (deleteTarget === null) return;
    // Closed only after the server confirms; on failure the dialog stays open
    // with an error and the comment stays listed.
    if (await removal.remove(deleteTarget)) setDeleteTarget(null);
  }

  // Continuation is armed only after "More"; before that, a visible sentinel
  // must not fetch anything.
  const sentinelRef = useInfiniteScrollTrigger<HTMLDivElement>({
    enabled:
      expanded &&
      data !== undefined &&
      hasNextPage &&
      !isFetchingNextPage &&
      !isFetchNextPageError,
    onIntersect: () => void fetchNextPage(),
  });

  const total = data?.pages[0]?.total ?? commentCount;
  const comments = expanded
    ? (data?.pages.flatMap((page) => page.items) ?? [])
    : (data?.pages[0]?.items.slice(0, INITIAL_COMMENT_COUNT) ?? []);
  const showMore = !expanded && data !== undefined && total > INITIAL_COMMENT_COUNT;
  const now = Date.now();

  function expand() {
    setExpanded(true);
    // Collapsed state holds only the initial page: start the continuation.
    if (data?.pages.length === 1 && hasNextPage) void fetchNextPage();
  }

  return (
    <section className={styles.section} aria-labelledby="comments-heading">
      <h2 id="comments-heading" className={styles.heading}>
        댓글
        <span className={styles.headingCount}>
          ({total.toLocaleString("ko-KR")})
        </span>
      </h2>

      {status === "pending" && (
        <p className={styles.status} role="status">
          댓글을 불러오는 중입니다.
        </p>
      )}

      {/* A failed continuation also sets status to "error"; it is reported
          below the loaded comments instead. */}
      {status === "error" && data === undefined && (
        <div className={styles.status} role="alert">
          <p className={styles.statusText}>댓글을 불러오지 못했습니다.</p>
          <button
            type="button"
            className={styles.retry}
            onClick={() => void refetch()}
          >
            다시 시도
          </button>
        </div>
      )}

      {comments.length > 0 && (
        <ul className={styles.list}>
          {comments.map((comment) => {
            const relativeTime = formatRelativeTime(comment.createdAt, now);
            return (
              <li key={comment.id} className={styles.comment}>
                {/* Decorative: the author name follows as text. */}
                <img
                  src={avatarFor(comment.author)}
                  alt=""
                  width={30}
                  height={30}
                  className={styles.avatar}
                />
                <div className={styles.body}>
                  <p className={styles.meta}>
                    <span
                      className={
                        comment.author === requester
                          ? `${styles.author} ${styles.authorOwn}`
                          : styles.author
                      }
                    >
                      {comment.author}
                    </span>
                    {relativeTime && (
                      <time dateTime={comment.createdAt} className={styles.time}>
                        {relativeTime}
                      </time>
                    )}
                  </p>
                  <p className={styles.content}>{comment.content}</p>
                  {comment.author === requester && (
                    <button
                      type="button"
                      className={styles.delete}
                      onClick={() => {
                        removal.reset();
                        setDeleteTarget(comment.id);
                      }}
                    >
                      삭제
                    </button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {showMore && (
        <button type="button" className={styles.more} onClick={expand}>
          댓글 더보기
          <ChevronDownIcon />
        </button>
      )}

      {isFetchingNextPage && (
        <p className={styles.status} role="status">
          댓글을 더 불러오는 중입니다.
        </p>
      )}

      {isFetchNextPageError && (
        <div className={styles.status} role="alert">
          <p className={styles.statusText}>다음 댓글을 불러오지 못했습니다.</p>
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

      <CommentComposer postId={postId} />

      {deleteTarget !== null && (
        <ConfirmDialog
          title="삭제하기"
          body={"삭제한 댓글은 복구할 수 없습니다.\n정말 삭제 하시겠습니까?"}
          pending={removal.isPending}
          error={removal.isError ? "댓글을 삭제하지 못했습니다. 다시 시도해 주세요." : null}
          onCancel={closeDialog}
          onConfirm={() => void confirmDelete()}
        />
      )}
    </section>
  );
}

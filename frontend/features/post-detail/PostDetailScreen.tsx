"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api/client";
import type { Post } from "@/lib/api/types";
import { avatarFor } from "@/lib/avatar";
import { rememberFormOrigin } from "@/lib/formOrigin";
import { readCurrentRequester } from "@/lib/requester";
import { routes } from "@/lib/routes";
import { formatShortDate } from "@/lib/time/shortDate";
import { CommentSection } from "./CommentSection";
import { ConfirmDialog } from "./ConfirmDialog";
import { BackIcon, HeartIcon, MoreIcon } from "./icons";
import { PostActionSheet } from "./PostActionSheet";
import { useDeletePost } from "./useDeletePost";
import { usePostDetail, useTogglePostLike } from "./usePostDetail";
import styles from "./PostDetailScreen.module.css";

type Props = {
  // null when the route segment is not a positive integer.
  postId: number | null;
};

export function PostDetailScreen({ postId }: Props) {
  const router = useRouter();
  const { data: post, status, error, refetch } = usePostDetail(postId);
  const notFound =
    postId === null || (error instanceof ApiError && error.status === 404);
  // Presentation only: the backend rejects another user's PATCH/DELETE (403).
  const [requester] = useState(readCurrentRequester);
  const isOwnPost = post !== undefined && !notFound && post.author === requester;
  const [menu, setMenu] = useState<"closed" | "sheet" | "confirmDelete">("closed");
  const removal = useDeletePost(postId ?? 0);
  const deleted = useRef(false);

  // Dropped only once this screen unmounts; removing the detail query while it
  // is still observed here would refetch the deleted post.
  useEffect(
    () => () => {
      if (deleted.current) removal.forgetDeletedPost();
    },
    [],
  );

  async function confirmDelete() {
    if (await removal.remove()) {
      deleted.current = true;
      router.replace(routes.home());
    }
  }

  function goBack() {
    // Direct loads have no in-app history to return to.
    if (window.history.length > 1) router.back();
    else router.push(routes.home());
  }

  return (
    <div className={styles.screen}>
      <header className={styles.nav}>
        <button
          type="button"
          className={styles.back}
          onClick={goBack}
          aria-label="뒤로 가기"
        >
          <BackIcon />
        </button>
        {isOwnPost && (
          <button
            type="button"
            className={styles.back}
            onClick={() => setMenu("sheet")}
            aria-label="게시글 메뉴"
            aria-haspopup="dialog"
          >
            <MoreIcon />
          </button>
        )}
      </header>

      <main className={styles.main}>
        {notFound ? (
          <div className={styles.notice} role="alert">
            <p className={styles.noticeText}>게시글을 찾을 수 없습니다.</p>
            <Link href={routes.home()} className={styles.action}>
              목록으로
            </Link>
          </div>
        ) : status === "pending" ? (
          <p className={styles.notice} role="status">
            게시글을 불러오는 중입니다.
          </p>
        ) : status === "error" ? (
          <div className={styles.notice} role="alert">
            <p className={styles.noticeText}>게시글을 불러오지 못했습니다.</p>
            <button
              type="button"
              className={styles.action}
              onClick={() => void refetch()}
            >
              다시 시도
            </button>
          </div>
        ) : (
          <>
            <PostBody post={post} />
            <CommentSection postId={post.id} commentCount={post.commentCount} />
          </>
        )}
      </main>

      {isOwnPost && menu === "sheet" && (
        <PostActionSheet
          onEdit={() => {
            rememberFormOrigin(routes.postDetail(post.id));
            router.push(routes.postEdit(post.id));
          }}
          onDelete={() => {
            removal.reset();
            setMenu("confirmDelete");
          }}
          onClose={() => setMenu("closed")}
        />
      )}
      {isOwnPost && menu === "confirmDelete" && (
        <ConfirmDialog
          title="삭제하기"
          body={"삭제한 글은 복구할 수 없습니다.\n정말 삭제 하시겠습니까?"}
          pending={removal.isPending}
          error={removal.isError ? "게시글을 삭제하지 못했습니다. 다시 시도해 주세요." : null}
          onCancel={() => {
            removal.reset();
            setMenu("closed");
          }}
          onConfirm={() => void confirmDelete()}
        />
      )}
    </div>
  );
}

function PostBody({ post }: { post: Post }) {
  const like = useTogglePostLike(post.id);

  const date = formatShortDate(post.createdAt);

  return (
    <article className={styles.post}>
      <h1 className={styles.title}>{post.title}</h1>
      <div className={styles.byline}>
        {/* Decorative: the author name follows as text. */}
        <img
          src={avatarFor(post.author)}
          alt=""
          width={36}
          height={36}
          className={styles.avatar}
        />
        <div>
          <p className={styles.author}>{post.author}</p>
          {date && (
            <time dateTime={post.createdAt} className={styles.date}>
              {date}
            </time>
          )}
        </div>
      </div>
      <p className={styles.content}>{post.content}</p>

      <div className={styles.likeRow}>
        <button
          type="button"
          className={styles.like}
          data-liked={post.isLiked}
          aria-pressed={post.isLiked}
          disabled={like.isPending}
          onClick={like.toggle}
        >
          <span className={styles.heart}>
            <HeartIcon filled={post.isLiked} />
          </span>
          좋아요 {post.likeCount.toLocaleString("ko-KR")}
        </button>
        {like.isError && (
          <p className={styles.likeError} role="alert">
            좋아요를 반영하지 못했습니다. 다시 시도해 주세요.
          </p>
        )}
      </div>
    </article>
  );
}

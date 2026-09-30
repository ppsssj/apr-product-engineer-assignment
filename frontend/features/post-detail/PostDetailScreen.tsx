"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState, type RefObject } from "react";
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
import { PostActionMenu } from "./PostActionMenu";
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
  const [menu, setMenu] = useState<"closed" | "actions" | "confirmDelete">("closed");
  const menuTriggerRef = useRef<HTMLButtonElement>(null);
  const closeMenu = useCallback(() => setMenu("closed"), []);
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
        {/* Screen identity, not the post heading (the post title is the h1). */}
        <p className={styles.appTitle}>메디큐브톡</p>
        {/* Keeps the title centered; post actions live on the post itself. */}
        <span className={styles.navSpacer} aria-hidden="true" />
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
            <PostBody
              post={post}
              ownerMenu={
                isOwnPost
                  ? {
                      open: menu === "actions",
                      triggerRef: menuTriggerRef,
                      onToggle: () =>
                        setMenu((current) => (current === "actions" ? "closed" : "actions")),
                      onClose: closeMenu,
                      onEdit: () => {
                        setMenu("closed");
                        rememberFormOrigin(routes.postDetail(post.id));
                        router.push(routes.postEdit(post.id));
                      },
                      onDelete: () => {
                        removal.reset();
                        // The pressed item unmounts with the menu; focusing the
                        // trigger first lets the dialog return focus to it.
                        menuTriggerRef.current?.focus();
                        setMenu("confirmDelete");
                      },
                    }
                  : undefined
              }
            />
            <CommentSection postId={post.id} commentCount={post.commentCount} />
          </>
        )}
      </main>

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

type OwnerMenu = {
  open: boolean;
  triggerRef: RefObject<HTMLButtonElement | null>;
  onToggle: () => void;
  onClose: () => void;
  onEdit: () => void;
  onDelete: () => void;
};

type PostBodyProps = {
  post: Post;
  // Present only for the requester's own post; edit/delete act on this post,
  // so the trigger and its popover sit in the post's author row rather than
  // the app bar.
  ownerMenu?: OwnerMenu;
};

const POST_ACTION_MENU_ID = "post-action-menu";

function PostBody({ post, ownerMenu }: PostBodyProps) {
  const like = useTogglePostLike(post.id);

  const date = formatShortDate(post.createdAt);

  return (
    <article className={styles.post}>
      <div className={styles.byline}>
        {/* Decorative: the author name follows as text. */}
        <img
          src={avatarFor(post.author)}
          alt=""
          width={24}
          height={24}
          className={styles.avatar}
        />
        <p className={styles.author}>{post.author}</p>
        {ownerMenu && (
          <div className={styles.menuAnchor}>
            <button
              ref={ownerMenu.triggerRef}
              type="button"
              className={styles.menu}
              onClick={ownerMenu.onToggle}
              aria-label="게시글 메뉴"
              aria-expanded={ownerMenu.open}
              aria-controls={ownerMenu.open ? POST_ACTION_MENU_ID : undefined}
            >
              <MoreIcon />
            </button>
            {ownerMenu.open && (
              <PostActionMenu
                id={POST_ACTION_MENU_ID}
                triggerRef={ownerMenu.triggerRef}
                onEdit={ownerMenu.onEdit}
                onDelete={ownerMenu.onDelete}
                onClose={ownerMenu.onClose}
              />
            )}
          </div>
        )}
      </div>
      <h1 className={styles.title}>{post.title}</h1>
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
        {date && (
          <time dateTime={post.createdAt} className={styles.date}>
            {date}
          </time>
        )}
        {like.isError && (
          <p className={styles.likeError} role="alert">
            좋아요를 반영하지 못했습니다. 다시 시도해 주세요.
          </p>
        )}
      </div>
    </article>
  );
}

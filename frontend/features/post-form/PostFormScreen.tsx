"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError } from "@/lib/api/client";
import { setFlash } from "@/lib/flash";
import { takeFormOrigin } from "@/lib/formOrigin";
import { readCurrentRequester } from "@/lib/requester";
import { routes } from "@/lib/routes";
import { BackIcon } from "@/features/post-detail/icons";
import { usePostDetail } from "@/features/post-detail/usePostDetail";
import { PostForm } from "./PostForm";
import { useCreatePost, useUpdatePost } from "./usePostFormMutations";
import styles from "./PostFormScreen.module.css";

export const POST_CREATED_MESSAGE = "게시글이 등록되었습니다.";

function Shell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  function goBack() {
    // Direct loads have no in-app history to return to.
    if (window.history.length > 1) router.back();
    else router.push(routes.home());
  }
  return (
    <div className={styles.screen}>
      <header className={styles.nav}>
        <button type="button" className={styles.back} onClick={goBack} aria-label="뒤로 가기">
          <BackIcon />
        </button>
      </header>
      <main>{children}</main>
    </div>
  );
}

export function CreatePostScreen() {
  const router = useRouter();
  const create = useCreatePost();
  return (
    <Shell>
      <PostForm
        heading="글작성"
        submitLabel="작성 완료"
        pending={create.isPending}
        errorMessage={create.isError ? "게시글을 등록하지 못했습니다. 다시 시도해 주세요." : null}
        onSubmit={async (input) => {
          const post = await create.submit(input);
          if (!post) return false;
          // The list shows this once after the client-side navigation.
          setFlash(POST_CREATED_MESSAGE);
          if (takeFormOrigin(routes.home())) router.back();
          else router.replace(routes.home());
          return true;
        }}
      />
    </Shell>
  );
}

export function EditPostScreen({ postId }: { postId: number | null }) {
  const { data: post, status, error, refetch } = usePostDetail(postId);
  // Presentation only: the backend rejects another user's PATCH with 403.
  const [requester] = useState(readCurrentRequester);
  const notFound = postId === null || (error instanceof ApiError && error.status === 404);

  if (notFound) {
    return (
      <Shell>
        <Notice text="게시글을 찾을 수 없습니다.">
          <Link href={routes.home()} className={styles.action}>
            목록으로
          </Link>
        </Notice>
      </Shell>
    );
  }
  if (status === "pending") {
    return (
      <Shell>
        <p className={styles.notice} role="status">
          게시글을 불러오는 중입니다.
        </p>
      </Shell>
    );
  }
  if (status === "error") {
    return (
      <Shell>
        <Notice text="게시글을 불러오지 못했습니다.">
          <button type="button" className={styles.action} onClick={() => void refetch()}>
            다시 시도
          </button>
        </Notice>
      </Shell>
    );
  }
  if (post.author !== requester) {
    return (
      <Shell>
        <Notice text="이 게시글을 수정할 권한이 없습니다.">
          <Link href={routes.postDetail(post.id)} className={styles.action}>
            게시글로
          </Link>
        </Notice>
      </Shell>
    );
  }
  // Keyed by id so the form takes the loaded values as its initial state once;
  // a background refetch does not overwrite what the user is typing.
  return <EditForm key={post.id} postId={post.id} title={post.title} content={post.content} />;
}

function EditForm({ postId, title, content }: { postId: number; title: string; content: string }) {
  const router = useRouter();
  const update = useUpdatePost(postId);
  return (
    <Shell>
      <PostForm
        heading="글수정"
        submitLabel="수정 완료"
        initialTitle={title}
        initialContent={content}
        pending={update.isPending}
        errorMessage={update.isError ? "게시글을 수정하지 못했습니다. 다시 시도해 주세요." : null}
        onSubmit={async (input) => {
          const post = await update.submit(input);
          if (!post) return false;
          const detail = routes.postDetail(post.id);
          if (takeFormOrigin(detail)) router.back();
          else router.replace(detail);
          return true;
        }}
      />
    </Shell>
  );
}

function Notice({ text, children }: { text: string; children: React.ReactNode }) {
  return (
    <div className={styles.notice} role="alert">
      <p className={styles.noticeText}>{text}</p>
      {children}
    </div>
  );
}

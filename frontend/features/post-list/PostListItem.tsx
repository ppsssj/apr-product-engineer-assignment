import Link from "next/link";
import type { Post } from "@/lib/api/types";
import { avatarFor } from "@/lib/avatar";
import { routes } from "@/lib/routes";
import { formatRelativeTime } from "@/lib/time/relativeTime";
import { CommentIcon, LikeIcon } from "./icons";
import styles from "./PostListItem.module.css";

type Props = {
  post: Post;
  now: number;
};

export function PostListItem({ post, now }: Props) {
  const relativeTime = formatRelativeTime(post.createdAt, now);

  return (
    <li className={styles.row}>
      <Link
        href={routes.postDetail(post.id)}
        className={styles.link}
        // Detail route arrives in P5; skip prefetching a 404 until then.
        prefetch={false}
      >
        <div className={styles.text}>
          <h2 className={styles.title}>{post.title}</h2>
          <p className={styles.preview}>{post.content}</p>
        </div>
        <div className={styles.meta}>
          <span className={styles.author}>
            {/* Decorative: the author name follows as text. */}
            <img
              src={avatarFor(post.author)}
              alt=""
              width={18}
              height={18}
              className={styles.avatar}
            />
            <span className={styles.authorName}>{post.author}</span>
          </span>
          <span className={styles.count}>
            <CommentIcon />
            <span className={styles.srOnly}>댓글</span>
            {post.commentCount}
          </span>
          <span className={styles.count}>
            <LikeIcon />
            <span className={styles.srOnly}>좋아요</span>
            {post.likeCount}
          </span>
          {relativeTime && (
            <time dateTime={post.createdAt} className={styles.time}>
              {relativeTime}
            </time>
          )}
        </div>
      </Link>
    </li>
  );
}

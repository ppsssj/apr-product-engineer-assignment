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
      {/* No viewport prefetch: /posts/[postId] is dynamic, so each row's
          prefetch returned only a tiny shell and every click still fetched the
          route itself. In an infinite list that was one request per row for no
          faster navigation. */}
      <Link href={routes.postDetail(post.id)} className={styles.link} prefetch={false}>
        {/* Retouch: content first; the author is part of the metadata row. */}
        <h2 className={styles.title}>{post.title}</h2>
        <p className={styles.preview}>{post.content}</p>
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
          <span className={styles.engagement}>
            <span className={styles.count}>
              <span className={styles.icon}>
                <CommentIcon />
              </span>
              <span className={styles.srOnly}>댓글</span>
              {post.commentCount.toLocaleString("ko-KR")}
            </span>
            <span className={styles.count}>
              <span className={post.isLiked ? styles.likedIcon : styles.icon}>
                <LikeIcon />
              </span>
              <span className={styles.srOnly}>좋아요</span>
              {post.likeCount.toLocaleString("ko-KR")}
            </span>
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

import { PostDetailScreen } from "@/features/post-detail/PostDetailScreen";
import { parsePostId } from "@/lib/postId";

export default async function PostDetailPage({
  params,
}: {
  params: Promise<{ postId: string }>;
}) {
  const { postId } = await params;
  return <PostDetailScreen postId={parsePostId(postId)} />;
}

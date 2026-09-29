import { PostDetailScreen } from "@/features/post-detail/PostDetailScreen";

function parsePostId(segment: string): number | null {
  return /^[1-9]\d*$/.test(segment) && Number.isSafeInteger(Number(segment))
    ? Number(segment)
    : null;
}

export default async function PostDetailPage({
  params,
}: {
  params: Promise<{ postId: string }>;
}) {
  const { postId } = await params;
  return <PostDetailScreen postId={parsePostId(postId)} />;
}

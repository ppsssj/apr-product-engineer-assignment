import { EditPostScreen } from "@/features/post-form/PostFormScreen";
import { parsePostId } from "@/lib/postId";

export default async function EditPostPage({
  params,
}: {
  params: Promise<{ postId: string }>;
}) {
  const { postId } = await params;
  return <EditPostScreen postId={parsePostId(postId)} />;
}

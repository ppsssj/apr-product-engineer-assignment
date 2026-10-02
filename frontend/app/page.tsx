import { cookies } from "next/headers";
import { PostListScreen } from "@/features/post-list/PostListScreen";
import { POST_LIST_PAGE_SIZE } from "@/features/post-list/postListConfig";
import { getInitialPostPage } from "@/lib/api/server";

// The default list's first page is fetched here so the HTML already contains
// it; reading the requester cookie makes this route dynamic. Later pages,
// sorts, and searches stay client-side React Query requests.
export default async function Home() {
  const requester = (await cookies()).get("x-user-id")?.value;
  const initialPage = await getInitialPostPage(POST_LIST_PAGE_SIZE, requester);
  return <PostListScreen initialPage={initialPage} />;
}

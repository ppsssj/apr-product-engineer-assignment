import { expect, test, type APIResponse, type Page, type Response } from "@playwright/test";

type Post = {
  id: number;
  title: string;
  content: string;
  likeCount: number;
  commentCount: number;
  isLiked: boolean;
  author: string;
  createdAt: string;
};

type Comment = {
  id: number;
  postId: number;
  content: string;
  author: string;
  createdAt: string;
};

type PageResponse<T> = {
  items: T[];
  total: number;
  nextCursor: string | null;
};

const apiBaseURL = (process.env.E2E_API_BASE_URL ?? "http://localhost:8080/api").replace(
  /\/+$/,
  "",
);
const appBaseURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";

function isPostsResponse(response: Response, expected: { query?: string; sort?: string }): boolean {
  const url = new URL(response.url());
  return (
    response.request().method() === "GET" &&
    url.pathname === "/api/posts" &&
    (expected.query === undefined || url.searchParams.get("q") === expected.query) &&
    (expected.sort === undefined || url.searchParams.get("sort") === expected.sort)
  );
}

function isCommentsResponse(
  response: Response,
  postId: number,
  expected: { limit: string; continuation: boolean },
): boolean {
  const url = new URL(response.url());
  const cursor = url.searchParams.get("cursor");
  return (
    response.request().method() === "GET" &&
    url.pathname === `/api/posts/${postId}/comments` &&
    url.searchParams.get("limit") === expected.limit &&
    (expected.continuation ? Boolean(cursor) : cursor === null)
  );
}

async function expectNoHorizontalOverflow(page: Page) {
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= document.documentElement.clientWidth,
      ),
    )
    .toBe(true);
}

async function cleanupPost(
  cleanup: () => Promise<APIResponse>,
  originalError: unknown,
): Promise<void> {
  try {
    const response = await cleanup();
    if (response.status() !== 204) {
      throw new Error(`Cleanup DELETE returned ${response.status()}: ${await response.text()}`);
    }
  } catch (cleanupError) {
    if (originalError !== undefined) {
      console.error("Cleanup failed after the test had already failed:", cleanupError);
      return;
    }
    throw cleanupError;
  }
}

test("seed-backed post search, likes sorting, and mobile controls work", async ({ page }) => {
  // Stable seed invariant: post #2 contains this unique phrase in its title.
  const searchTerm = "울트라 톤 실";

  await page.goto("/");

  const main = page.getByRole("main");
  const postLinks = main.locator('a[href^="/posts/"]');
  await expect(postLinks.first()).toBeVisible();

  const searchTrigger = page.getByRole("button", { name: "게시글 검색" });
  const createLink = page.getByRole("link", { name: "게시글 작성" });
  await expect(searchTrigger).toBeVisible();
  await expect(searchTrigger).toBeEnabled();
  await expect(createLink).toBeVisible();
  await expect(createLink).toBeEnabled();

  await searchTrigger.click();
  const searchInput = page.getByRole("textbox", { name: "게시글 검색" });
  const searchResponsePromise = page.waitForResponse((response) =>
    isPostsResponse(response, { query: searchTerm }),
  );
  await searchInput.fill(searchTerm);
  const searchResponse = await searchResponsePromise;
  expect(searchResponse.ok()).toBe(true);

  const searchPage = (await searchResponse.json()) as PageResponse<Post>;
  expect(searchPage.items.length).toBeGreaterThan(0);
  expect(
    searchPage.items.every((post) => `${post.title} ${post.content}`.includes(searchTerm)),
  ).toBe(true);
  await expect(postLinks).toHaveCount(searchPage.items.length);
  for (const post of searchPage.items) {
    await expect(main.locator(`a[href="/posts/${post.id}"]`)).toBeVisible();
  }

  await searchInput.press("Escape");
  const likesSort = page.getByRole("button", { name: "좋아요순" });
  await expect(likesSort).toBeVisible();

  const likesResponsePromise = page.waitForResponse((response) =>
    isPostsResponse(response, { sort: "likes" }),
  );
  await likesSort.click();
  const likesResponse = await likesResponsePromise;
  expect(likesResponse.ok()).toBe(true);
  await expect(likesSort).toHaveAttribute("aria-pressed", "true");

  const likesPage = (await likesResponse.json()) as PageResponse<Post>;
  expect(likesPage.items.length).toBeGreaterThan(1);
  for (let index = 1; index < likesPage.items.length; index += 1) {
    expect(likesPage.items[index - 1].likeCount).toBeGreaterThanOrEqual(
      likesPage.items[index].likeCount,
    );
  }
  await expect(postLinks).toHaveCount(likesPage.items.length);
  const renderedIds = await postLinks.evaluateAll((links) =>
    links.map((link) => Number(new URL((link as HTMLAnchorElement).href).pathname.split("/").pop())),
  );
  expect(renderedIds).toEqual(likesPage.items.map((post) => post.id));

  await expectNoHorizontalOverflow(page);
  await expect(page.getByRole("group", { name: "정렬" })).toBeVisible();
  await expect(createLink).toBeVisible();
});

test("seed post #3 keeps the initial-two comments gate across an SPA revisit", async ({ page }) => {
  // Stable seed invariant: post #3 has exactly three readable comments.
  const postId = 3;
  const commentsPath = `/api/posts/${postId}/comments`;
  const commentRequests: string[] = [];
  page.on("request", (request) => {
    const url = new URL(request.url());
    if (request.method() === "GET" && url.pathname === commentsPath) {
      commentRequests.push(request.url());
    }
  });

  await page.goto("/");
  const detailLink = page.locator(`main a[href="/posts/${postId}"]`);
  await expect(detailLink).toBeVisible();

  const initialResponsePromise = page.waitForResponse((response) =>
    isCommentsResponse(response, postId, { limit: "2", continuation: false }),
  );
  await detailLink.click();
  await page.waitForURL(`**/posts/${postId}`);
  const initialResponse = await initialResponsePromise;
  expect(initialResponse.ok()).toBe(true);

  const initialPage = (await initialResponse.json()) as PageResponse<Comment>;
  expect(initialPage.total).toBe(3);
  expect(initialPage.items).toHaveLength(2);
  expect(initialPage.nextCursor).not.toBeNull();

  const commentsRegion = page.getByRole("region", { name: /댓글/ });
  const commentItems = commentsRegion.getByRole("listitem");
  const moreButton = page.getByRole("button", { name: "댓글 더보기" });
  await expect(commentItems).toHaveCount(2);
  await expect(moreButton).toBeVisible();
  expect(
    commentRequests.some((requestUrl) => Boolean(new URL(requestUrl).searchParams.get("cursor"))),
  ).toBe(false);

  const continuationResponsePromise = page.waitForResponse((response) =>
    isCommentsResponse(response, postId, { limit: "30", continuation: true }),
  );
  await moreButton.click();
  const continuationResponse = await continuationResponsePromise;
  expect(continuationResponse.ok()).toBe(true);
  const continuationPage = (await continuationResponse.json()) as PageResponse<Comment>;
  expect(continuationPage.items).toHaveLength(1);
  await expect(commentItems).toHaveCount(3);

  await page.getByRole("button", { name: "뒤로 가기" }).click();
  await page.waitForURL((url) => url.pathname === "/");
  await expect(detailLink).toBeVisible();

  const revisitResponsePromise = page.waitForResponse((response) =>
    isCommentsResponse(response, postId, { limit: "2", continuation: false }),
  );
  await detailLink.click();
  await page.waitForURL(`**/posts/${postId}`);
  const revisitResponse = await revisitResponsePromise;
  expect(revisitResponse.ok()).toBe(true);
  await expect(commentItems).toHaveCount(2);
  await expect(moreButton).toBeVisible();

  await expectNoHorizontalOverflow(page);
  const composer = page.getByRole("textbox", { name: "댓글 입력" });
  await expect(composer).toBeVisible();
  await expect(composer).toBeEnabled();
});

test("a disposable post delete dialog traps focus and cancels without mutation", async ({
  context,
  page,
  request,
}) => {
  const unique = Date.now().toString(36);
  const requester = `e2e_dialog_${unique}`;
  const title = `e2e-${unique}`;
  const headers = { "x-user-id": requester };
  const createResponse = await request.post(`${apiBaseURL}/posts`, {
    headers,
    data: { title, content: "Playwright delete-dialog keyboard fixture" },
  });
  expect(createResponse.status()).toBe(201);
  const post = (await createResponse.json()) as Post;

  let originalError: unknown;
  try {
    await context.addCookies([
      {
        name: "x-user-id",
        value: requester,
        url: appBaseURL,
      },
    ]);

    let browserDeleteRequests = 0;
    page.on("request", (browserRequest) => {
      const url = new URL(browserRequest.url());
      if (
        browserRequest.method() === "DELETE" &&
        url.pathname === `/api/posts/${post.id}`
      ) {
        browserDeleteRequests += 1;
      }
    });

    await page.goto(`/posts/${post.id}`);
    await expect(page.getByRole("heading", { level: 1, name: title })).toBeVisible();

    const menuButton = page.getByRole("button", { name: "게시글 메뉴" });
    await menuButton.click();
    await page.getByRole("button", { name: "삭제하기" }).click();

    const dialog = page.getByRole("alertdialog", { name: "삭제하기" });
    const cancelButton = dialog.getByRole("button", { name: "취소" });
    const confirmButton = dialog.getByRole("button", { name: "예" });
    await expect(dialog).toBeVisible();
    await expect(cancelButton).toBeFocused();

    await page.keyboard.press("Tab");
    await expect(confirmButton).toBeFocused();
    await page.keyboard.press("Tab");
    await expect(cancelButton).toBeFocused();
    await page.keyboard.press("Shift+Tab");
    await expect(confirmButton).toBeFocused();
    await expect(dialog.locator(":focus")).toHaveCount(1);

    await page.keyboard.press("Escape");
    await expect(dialog).toBeHidden();
    await expect(menuButton).toBeFocused();
    expect(browserDeleteRequests).toBe(0);

    const readableResponse = await request.get(`${apiBaseURL}/posts/${post.id}`, { headers });
    expect(readableResponse.status(), await readableResponse.text()).toBe(200);
  } catch (error) {
    originalError = error;
    throw error;
  } finally {
    await cleanupPost(
      () => request.delete(`${apiBaseURL}/posts/${post.id}`, { headers }),
      originalError,
    );
  }
});

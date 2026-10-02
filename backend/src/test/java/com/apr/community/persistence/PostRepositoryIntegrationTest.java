package com.apr.community.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.pagination.PageRequestParser;
import com.apr.community.common.pagination.PageResponse;
import com.apr.community.post.PostListRequestParser;
import com.apr.community.post.PostReadService;
import com.apr.community.post.PostResponse;
import com.apr.community.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class PostRepositoryIntegrationTest extends PostgresIntegrationTest {

    private static final Instant T = Instant.parse("2099-01-01T00:00:00Z");

    @Autowired
    private PostReadService postReadService;

    @Autowired
    private PostListRequestParser postListRequestParser;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PageRequestParser pageRequestParser;

    @Test
    void latestPagesFollowCreatedAtThenIdAcrossTiesAndChangingPageSizes() {
        long older = insertPost("older", "x", "u", 0, T.minusSeconds(1), false);
        long tie1 = insertPost("tie1", "x", "u", 0, T, false);
        long tie2 = insertPost("tie2", "x", "u", 0, T, false);
        insertPost("tie-deleted", "x", "u", 0, T, true);
        long tie3 = insertPost("tie3", "x", "u", 0, T, false);
        long tie4 = insertPost("tie4", "x", "u", 0, T, false);
        long newer = insertPost("newer", "x", "u", 0, T.plusSeconds(1), false);

        // limit 2 then 3 puts a page boundary inside the equal-created_at group.
        List<PostResponse> traversed = traverse("latest", null, 2, 3, 1, 5);

        List<Long> ids = ids(traversed);
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids).startsWith(newer, tie4, tie3, tie2, tie1, older);
        assertThat(ids).containsExactlyElementsOf(ids(expectedLatest(null)));
    }

    @Test
    void likesPagesFollowEffectiveLikeCountThenCreatedAtThenId() {
        String marker = "lkmark";
        long a = insertPost("a", marker, "u", 5, T, false);
        long b = insertPost("b", marker, "u", 3, T, false);
        insertLike(b, "liker-1");
        insertLike(b, "liker-2");
        long olderFive = insertPost("c", marker, "u", 5, T.minusSeconds(1), false);
        long newerFive = insertPost("d", marker, "u", 4, T.plusSeconds(1), false);
        insertLike(newerFive, "liker-1");
        long six = insertPost("e", marker, "u", 6, T.minusSeconds(5), false);
        insertPost("deleted", marker, "u", 100, T, true);
        long seedPost = 1L;
        insertLike(seedPost, "liker-1");

        List<PostResponse> all = traverse("likes", null, 3, 2, 4);
        assertThat(ids(all)).doesNotHaveDuplicates();
        assertThat(ids(all)).containsExactlyElementsOf(ids(expectedLikes(null)));

        // b's base (3) is lower than a's (5); only the effective count ties them.
        List<PostResponse> searched = traverse("likes", "LKMARK", 2, 1, 3);
        assertThat(ids(searched)).containsExactly(six, newerFive, b, a, olderFive);
        assertThat(searched).extracting(PostResponse::likeCount).containsExactly(6L, 5L, 5L, 5L, 5L);
        assertThat(ids(searched)).containsExactlyElementsOf(ids(expectedLikes("LKMARK")));
    }

    @Test
    void searchIsCaseInsensitiveSubstringOverTitleAndContentAndExcludesDeletedPosts() {
        long laserTitle = insertPost("Laser Pointer", "plain", "u", 0, T, false);
        long contentOnly = insertPost("plain", "has NEEDLEword inside", "u", 0, T, false);
        long deleted = insertPost("Laser deleted", "NEEDLEword", "u", 0, T, true);

        assertThat(ids(search("laser"))).contains(laserTitle).doesNotContain(deleted, contentOnly);
        assertThat(ids(search("LASER"))).contains(laserTitle).doesNotContain(deleted);
        assertThat(ids(search("aser poi"))).containsExactly(laserTitle);
        assertThat(ids(search("dlewo"))).containsExactly(contentOnly);

        for (String query : List.of("laser", "LASER", "aser poi", "dlewo")) {
            assertSearchMatchesOracle(query);
        }
    }

    @Test
    void searchTreatsLikeWildcardsAndEscapeCharacterLiterally() {
        long percent = insertPost("rate 100% zq", "x", "u", 0, T, false);
        long percentDecoy = insertPost("rate 1000 zq", "x", "u", 0, T, false);
        long underscore = insertPost("zq a_b", "x", "u", 0, T, false);
        long underscoreDecoy = insertPost("zq axb", "x", "u", 0, T, false);
        long backslash = insertPost("zq c:\\d", "x", "u", 0, T, false);
        long backslashDecoy = insertPost("zq c:d", "x", "u", 0, T, false);

        assertThat(ids(search("100%"))).contains(percent).doesNotContain(percentDecoy);
        assertThat(ids(search("a_b"))).contains(underscore).doesNotContain(underscoreDecoy);
        assertThat(ids(search("c:\\d"))).contains(backslash).doesNotContain(backslashDecoy);

        for (String query : List.of("100%", "a_b", "c:\\d", "%", "_", "\\")) {
            assertSearchMatchesOracle(query);
        }
    }

    @Test
    void aggregatesCombineBaseAndRelationalLikesReadableCommentsAndRequesterLike() {
        String marker = "aggmark";
        long post = insertPost("agg", marker, "author", 7, T, false);
        insertLike(post, "user-a");
        insertLike(post, "user-b");
        insertComment(post, "c1", "u", T, false);
        insertComment(post, "c2", "u", T, false);
        insertComment(post, "gone", "u", T, true);
        long other = insertPost("other", marker, "author", 0, T.minusSeconds(1), false);
        insertLike(other, "user-c");

        var asA = postReadService.getPost(post, "user-a");
        var asC = postReadService.getPost(post, "user-c");
        assertThat(asA.likeCount()).isEqualTo(9);
        assertThat(asA.commentCount()).isEqualTo(2);
        assertThat(asA.isLiked()).isTrue();
        assertThat(asC.likeCount()).isEqualTo(9);
        assertThat(asC.commentCount()).isEqualTo(2);
        assertThat(asC.isLiked()).isFalse();

        for (String sort : List.of("latest", "likes")) {
            var pageForA = postReadService.getPosts("user-a", postListRequestParser.parse(50, null, sort, marker));
            var pageForC = postReadService.getPosts("user-c", postListRequestParser.parse(50, null, sort, marker));
            var forA = byId(pageForA.items());
            var forC = byId(pageForC.items());

            assertThat(forA.get(post)).isEqualTo(asA);
            assertThat(forC.get(post)).isEqualTo(asC);
            assertThat(forA.get(other).isLiked()).isFalse();
            assertThat(forC.get(other).isLiked()).isTrue();
        }
    }

    @Test
    void softDeletedPostIsUnreadableEverywhere() {
        String marker = "delmark";
        long deleted = insertPost("gone", marker, "u", 0, T, false);
        insertComment(deleted, "c", "u", T, false);
        postRepository.softDelete(deleted);

        assertThat(isDeletedPost(deleted)).isTrue();
        assertThat(ids(traverse("latest", null, 50))).doesNotContain(deleted);
        assertThat(ids(traverse("likes", null, 50))).doesNotContain(deleted);
        var searched = postReadService.getPosts("u", postListRequestParser.parse(50, null, "latest", marker));
        assertThat(searched.items()).isEmpty();
        assertThat(searched.total()).isZero();

        assertNotFound(() -> postReadService.getPost(deleted, "u"));
        assertNotFound(() -> postReadService.getComments(deleted, pageRequestParser.parse(10, null)));
    }

    private void assertSearchMatchesOracle(String query) {
        List<PostResponse> expected = expectedLatest(query);
        var firstPage = postReadService.getPosts("u", postListRequestParser.parse(50, null, "latest", query));
        assertThat(firstPage.total()).as("total for %s", query).isEqualTo(expected.size());
        assertThat(ids(search(query))).as("results for %s", query).containsExactlyElementsOf(ids(expected));
    }

    private List<PostResponse> search(String query) {
        return traverse("latest", query, 50);
    }

    // Follows nextCursor to the end, cycling through the given page sizes.
    private List<PostResponse> traverse(String sort, String query, int... limits) {
        List<PostResponse> items = new ArrayList<>();
        String cursor = null;
        Long total = null;
        for (int page = 0; ; page++) {
            assertThat(page).as("page count").isLessThan(500);
            int limit = limits[page % limits.length];
            PageResponse<PostResponse> response = postReadService.getPosts(
                    "u", postListRequestParser.parse(limit, cursor, sort, query));

            if (total == null) {
                total = response.total();
            }
            assertThat(response.total()).isEqualTo(total);
            assertThat(response.items()).hasSizeLessThanOrEqualTo(limit);
            items.addAll(response.items());
            if (response.nextCursor() == null) {
                break;
            }
            assertThat(response.items()).hasSize(limit);
            cursor = response.nextCursor();
        }
        assertThat(items).hasSize(total.intValue());
        return items;
    }

    private List<PostResponse> expectedLatest(String query) {
        return readableRows(query).stream()
                .sorted(Comparator.comparing(PostResponse::createdAt).reversed()
                        .thenComparing(Comparator.comparingLong(PostResponse::id).reversed()))
                .toList();
    }

    private List<PostResponse> expectedLikes(String query) {
        return readableRows(query).stream()
                .sorted(Comparator.comparingLong(PostResponse::likeCount).reversed()
                        .thenComparing(Comparator.comparing(PostResponse::createdAt).reversed())
                        .thenComparing(Comparator.comparingLong(PostResponse::id).reversed()))
                .toList();
    }

    // Oracle computed in Java from raw rows, independent of the repository SQL.
    private List<PostResponse> readableRows(String query) {
        Map<Long, Long> likes = jdbcClient.sql("SELECT post_id FROM post_likes")
                .query(Long.class)
                .list()
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String needle = query == null ? null : query.toLowerCase(Locale.ROOT);

        return jdbcClient.sql("SELECT id, title, content, base_like_count, created_at, is_deleted FROM posts")
                .query((rs, rowNumber) -> rs.getBoolean("is_deleted") ? null : new PostResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getInt("base_like_count") + likes.getOrDefault(rs.getLong("id"), 0L),
                        0,
                        false,
                        null,
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list()
                .stream()
                .filter(row -> row != null)
                .filter(row -> needle == null
                        || row.title().toLowerCase(Locale.ROOT).contains(needle)
                        || row.content().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private static List<Long> ids(List<PostResponse> posts) {
        return posts.stream().map(PostResponse::id).toList();
    }

    private static Map<Long, PostResponse> byId(List<PostResponse> posts) {
        return posts.stream().collect(Collectors.toMap(PostResponse::id, Function.identity()));
    }

    private static void assertNotFound(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}

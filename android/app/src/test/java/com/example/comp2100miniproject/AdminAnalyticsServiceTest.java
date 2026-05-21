package com.example.comp2100miniproject;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import hashtag.HashtagService;
import moderation.PostReportRepository;
import postview.PostViewService;

import static org.junit.Assert.*;

/**
 * Unit tests for AdminAnalyticsService.
 * Message.java is not involved — all tests cover post-level and user-level analytics.
 */
public class AdminAnalyticsServiceTest {

    @Before
    public void setUp() {
        PostDAO.getInstance().clear();
        PostViewService.getInstance().clear();
        PostReportRepository.getInstance().clear();
        HashtagService.getInstance().clear();
    }

    private Post addPost(UUID poster, String topic) {
        Post p = new Post(UUID.randomUUID(), poster, topic);
        PostDAO.getInstance().add(p);
        return p;
    }

    @Test
    public void totalPostsCountsOnlyNonDeleted() {
        UUID user = UUID.randomUUID();
        addPost(user, "visible");
        Post deleted = addPost(user, "deleted");
        deleted.setDeleted(true);

        assertEquals(1, AdminAnalyticsService.getTotalPosts());
    }

    @Test
    public void postsToday_newPostCounts() {
        UUID user = UUID.randomUUID();
        addPost(user, "today post");
        // createdAt defaults to System.currentTimeMillis() so this is always "today"
        assertTrue(AdminAnalyticsService.getPostsToday() >= 1);
    }

    @Test
    public void totalRepliesCountsAllMessages() {
        UUID user = UUID.randomUUID();
        Post p = addPost(user, "post with replies");
        p.messages.insert(new dao.model.Message(
                UUID.randomUUID(), user, p.id, System.currentTimeMillis(), "reply 1"));
        p.messages.insert(new dao.model.Message(
                UUID.randomUUID(), user, p.id, System.currentTimeMillis(), "reply 2"));

        assertEquals(2, AdminAnalyticsService.getTotalReplies());
    }

    @Test
    public void mostViewedPostsOrdering() {
        UUID user = UUID.randomUUID();
        Post low  = addPost(user, "low views");
        Post high = addPost(user, "high views");
        PostViewService.getInstance().recordView(high.id);
        PostViewService.getInstance().recordView(high.id);
        PostViewService.getInstance().recordView(low.id);

        List<PostStat> result = AdminAnalyticsService.getMostViewedPosts(3);
        assertFalse(result.isEmpty());
        assertEquals(high.id, result.get(0).postId);
        assertEquals(2, result.get(0).viewCount);
    }

    @Test
    public void mostActiveUsersOrdering() {
        UUID alice = UUID.randomUUID();
        UUID bob   = UUID.randomUUID();
        addPost(alice, "alice 1");
        addPost(alice, "alice 2");
        addPost(alice, "alice 3");
        addPost(bob,   "bob 1");

        List<UserStat> result = AdminAnalyticsService.getMostActiveUsers(5);
        assertFalse(result.isEmpty());
        // alice has 3 posts, should be first
        assertEquals(3, result.get(0).postCount);
    }

    @Test
    public void pendingReportCountReflectsRepository() {
        assertEquals(0, AdminAnalyticsService.getPendingReportCount());

        UUID postId   = UUID.randomUUID();
        UUID reporter = UUID.randomUUID();
        UUID author   = UUID.randomUUID();
        PostReportRepository.getInstance().createReport(postId, reporter, author, "spam");

        assertEquals(1, AdminAnalyticsService.getPendingReportCount());
    }

    @Test
    public void deletedPostsExcludedFromPopularLists() {
        UUID user = UUID.randomUUID();
        Post deleted = addPost(user, "deleted post");
        PostViewService.getInstance().recordView(deleted.id);
        deleted.setDeleted(true);

        List<PostStat> viewed = AdminAnalyticsService.getMostViewedPosts(5);
        for (PostStat ps : viewed) {
            assertNotEquals(deleted.id, ps.postId);
        }
    }

    @Test
    public void computeStatsBundlesAllData() {
        UUID user = UUID.randomUUID();
        addPost(user, "post 1");
        addPost(user, "post 2");

        DashboardStats stats = AdminAnalyticsService.computeStats();
        assertEquals(2, stats.totalPosts);
        assertNotNull(stats.mostViewedPosts);
        assertNotNull(stats.mostReactedPosts);
        assertNotNull(stats.activeUsers);
    }
}

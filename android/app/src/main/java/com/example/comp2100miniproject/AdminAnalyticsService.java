package com.example.comp2100miniproject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import moderation.PostReportRepository;
import postview.PostViewService;

/**
 * Stateless analytics service that aggregates data from existing repositories
 * into a single {@link DashboardStats} snapshot for the Admin Dashboard.
 *
 * Design principles demonstrated:
 *  - Separation of concerns: all calculation lives here; Activities only render.
 *  - Reuse of existing data sources — no new data stores introduced.
 *  - Single Responsibility: only computes analytics, never modifies data.
 *  - Encapsulation: callers receive read-only DTOs.
 *
 * Message.java is NOT modified — message data is only READ, never written.
 * All stats are derived from the existing in-memory singletons (PostDAO,
 * UserDAO, PostViewService, ReactionManager, PostReportRepository).
 */
public final class AdminAnalyticsService {

    private static final int DEFAULT_TOP_N = 5;

    private AdminAnalyticsService() {}   // utility class

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Compute a full {@link DashboardStats} snapshot from the current
     * in-memory state of all repositories. Called once per dashboard open;
     * the result is then cached by the Activity until the next open/refresh.
     */
    public static DashboardStats computeStats() {
        return new DashboardStats(
                getTotalUsers(),
                getTotalPosts(),
                getTotalReplies(),
                getPostsToday(),
                getPendingReportCount(),
                getMostViewedPosts(DEFAULT_TOP_N),
                getMostReactedPosts(DEFAULT_TOP_N),
                getMostActiveUsers(DEFAULT_TOP_N)
        );
    }

    // ── Individual stat methods (package-visible for unit tests) ──────────────

    /** Count of all registered users in UserDAO (admin + members). */
    static int getTotalUsers() {
        int count = 0;
        for (Iterator<User> it = UserDAO.getInstance().getAll(); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

    /** Count of non-deleted posts in PostDAO. */
    static int getTotalPosts() {
        int count = 0;
        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            if (!it.next().isDeleted()) count++;
        }
        return count;
    }

    /**
     * Sum of all raw messages stored inside all posts (visible + hidden).
     * Uses PostDAO.getAllMessages() — Message.java is not modified.
     */
    static int getTotalReplies() {
        int count = 0;
        for (Iterator<Message> it = PostDAO.getInstance().getAllMessages(); it.hasNext(); it.next()) {
            count++;
        }
        return count;
    }

    /**
     * Posts whose in-memory {@link Post#getCreatedAt()} timestamp falls on
     * today's calendar date (device local time).
     */
    static int getPostsToday() {
        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayDay  = today.get(Calendar.DAY_OF_YEAR);

        int count = 0;
        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post p = it.next();
            if (p.isDeleted()) continue;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(p.getCreatedAt());
            if (c.get(Calendar.YEAR) == todayYear && c.get(Calendar.DAY_OF_YEAR) == todayDay) {
                count++;
            }
        }
        return count;
    }

    /** Number of PENDING entries in PostReportRepository. */
    static int getPendingReportCount() {
        return PostReportRepository.getInstance().getPendingReports().size();
    }

    /**
     * Top {@code limit} non-deleted posts by view count (PostViewService),
     * highest first.
     */
    static List<PostStat> getMostViewedPosts(int limit) {
        List<PostStat> stats = buildPostStats();
        stats.sort((a, b) -> Integer.compare(b.viewCount, a.viewCount));
        return Collections.unmodifiableList(
                stats.subList(0, Math.min(limit, stats.size())));
    }

    /**
     * Top {@code limit} non-deleted posts by total emoji reaction count
     * (ReactionManager), highest first.
     */
    static List<PostStat> getMostReactedPosts(int limit) {
        List<PostStat> stats = buildPostStats();
        stats.sort((a, b) -> Integer.compare(b.totalReactions, a.totalReactions));
        return Collections.unmodifiableList(
                stats.subList(0, Math.min(limit, stats.size())));
    }

    /**
     * Top {@code limit} users ranked by number of non-deleted posts they have
     * authored, highest first.
     */
    static List<UserStat> getMostActiveUsers(int limit) {
        Map<UUID, Integer> postCountByUser   = new HashMap<>();
        Map<UUID, String>  usernameByUserId  = new HashMap<>();

        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post p = it.next();
            if (p.isDeleted() || p.poster == null) continue;

            postCountByUser.merge(p.poster, 1, Integer::sum);

            if (!usernameByUserId.containsKey(p.poster)) {
                User u = UserDAO.getInstance().getByUUID(p.poster);
                usernameByUserId.put(p.poster,
                        u != null ? u.username() : p.poster.toString().substring(0, 8));
            }
        }

        List<UserStat> stats = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : postCountByUser.entrySet()) {
            stats.add(new UserStat(usernameByUserId.get(e.getKey()), e.getValue()));
        }
        stats.sort((a, b) -> Integer.compare(b.postCount, a.postCount));
        return Collections.unmodifiableList(
                stats.subList(0, Math.min(limit, stats.size())));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Build one PostStat per non-deleted post, reading views and reactions. */
    private static List<PostStat> buildPostStats() {
        List<PostStat> result = new ArrayList<>();
        ReactionManager rm = ReactionManager.getInstance();
        PostViewService  pv = PostViewService.getInstance();

        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post p = it.next();
            if (p.isDeleted()) continue;
            result.add(new PostStat(
                    p.id,
                    p.topic,
                    pv.getViewCount(p.id),
                    rm.getTotalReactionCount(p.id)));
        }
        return result;
    }
}

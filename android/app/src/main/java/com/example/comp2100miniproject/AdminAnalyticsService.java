package com.example.comp2100miniproject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.LinkedHashMap;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import messagestate.MessageDeletionRegistry;
import messagestate.MessageEditRegistry;
import moderation.BanRepository;
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

    // ── Full-list admin data (used by management list pages) ─────────────────

    /**
     * All users alphabetically — enriched with post count, reply count, ban status.
     * Reads UserDAO, PostDAO.getAllMessages() — Message.java NOT modified.
     */
    public static List<UserAdminSummary> getAllUsersForAdmin() {
        // Build reply counts per user by scanning messages once
        Map<UUID, Integer> repliesByUser = new HashMap<>();
        for (Iterator<Message> it = PostDAO.getInstance().getAllMessages(); it.hasNext(); ) {
            Message m = it.next();
            repliesByUser.merge(m.poster(), 1, Integer::sum);
        }
        // Build post counts per user by scanning posts once
        Map<UUID, Integer> postsByUser = new HashMap<>();
        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post p = it.next();
            if (!p.isDeleted() && p.poster != null)
                postsByUser.merge(p.poster, 1, Integer::sum);
        }

        List<UserAdminSummary> result = new ArrayList<>();
        BanRepository bans = BanRepository.getInstance();
        for (Iterator<User> it = UserDAO.getInstance().getAll(); it.hasNext(); ) {
            User u = it.next();
            result.add(new UserAdminSummary(
                    u.getUUID(),
                    u.username(),
                    u.role() == User.Role.Admin ? "Admin" : "Member",
                    postsByUser.getOrDefault(u.getUUID(), 0),
                    repliesByUser.getOrDefault(u.getUUID(), 0),
                    bans.isBanned(u.getUUID())
            ));
        }
        result.sort((a, b) -> a.username.compareToIgnoreCase(b.username));
        return Collections.unmodifiableList(result);
    }

    /** Same as {@link #getAllUsersForAdmin()} but sorted by post count descending. */
    public static List<UserAdminSummary> getActiveUsersRankingFull() {
        List<UserAdminSummary> ranked = new ArrayList<>(getAllUsersForAdmin());
        ranked.sort((a, b) -> Integer.compare(b.postCount, a.postCount));
        return Collections.unmodifiableList(ranked);
    }

    /**
     * All posts (including deleted) with admin metadata.
     * The {@code postIndex} field in each summary is the raw PostDAO iteration index
     * used by PostViewerActivity's "post_index" extra.
     * Message.java is not modified — reply counts read raw message storage.
     */
    public static List<PostAdminSummary> getAllPostsForAdmin() {
        ReactionManager rm = ReactionManager.getInstance();
        PostViewService  pv = PostViewService.getInstance();
        List<PostAdminSummary> result = new ArrayList<>();
        int rawIndex = 0;
        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); rawIndex++) {
            Post p = it.next();
            User author = UserDAO.getInstance().getByUUID(p.poster);
            String authorName = author != null ? author.username() : "?";
            int replyCount = 0;
            for (Iterator<Message> mi = p.messages.getAll(); mi.hasNext(); mi.next()) replyCount++;
            int reportCount = PostReportRepository.getInstance().getReportsForPost(p.id).size();
            result.add(new PostAdminSummary(
                    p.id, p.topic, authorName,
                    p.getCreatedAt(), pv.getViewCount(p.id), rm.getTotalReactionCount(p.id),
                    replyCount, reportCount, p.isDeleted(),
                    p.isDeleted() ? -1 : rawIndex,
                    p.getHashtags()
            ));
        }
        return Collections.unmodifiableList(result);
    }

    /** Only today's non-deleted posts. */
    public static List<PostAdminSummary> getTodaysPostsForAdmin() {
        Calendar today = Calendar.getInstance();
        int year = today.get(Calendar.YEAR);
        int day  = today.get(Calendar.DAY_OF_YEAR);
        List<PostAdminSummary> today_list = new ArrayList<>();
        for (PostAdminSummary ps : getAllPostsForAdmin()) {
            if (ps.deleted) continue;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(ps.createdAt);
            if (c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == day)
                today_list.add(ps);
        }
        return Collections.unmodifiableList(today_list);
    }

    /** Non-deleted posts sorted by (views + reactions) descending. */
    public static List<PostAdminSummary> getPopularPostsForAdmin() {
        List<PostAdminSummary> pop = new ArrayList<>();
        for (PostAdminSummary ps : getAllPostsForAdmin()) {
            if (!ps.deleted) pop.add(ps);
        }
        pop.sort((a, b) -> Integer.compare(
                b.viewCount + b.totalReactions,
                a.viewCount + a.totalReactions));
        return Collections.unmodifiableList(pop);
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

package moderation;

import dao.PostDAO;
import dao.model.Post;
import hashtag.HashtagService;

import java.util.List;
import java.util.UUID;

/**
 * Service layer for the Admin Post Moderation workflow.
 *
 * Orchestrates {@link PostReportRepository}, {@link BanRepository}, and
 * {@link dao.PostDAO} so that Activity/Fragment code stays thin — each UI
 * action maps to a single method call here.
 *
 * Design principles demonstrated:
 *  - Separation of concerns: business logic lives here, not in Activities.
 *  - Single Responsibility: only admin moderation orchestration.
 *  - Service pattern: stateless coordinator over multiple repositories.
 *  - Encapsulation: callers never touch the raw repositories directly.
 *
 * Message.java is not modified — all actions operate at Post or User level.
 */
public final class AdminModerationService {

    private static AdminModerationService instance;

    private AdminModerationService() {}

    public static AdminModerationService getInstance() {
        if (instance == null) instance = new AdminModerationService();
        return instance;
    }

    // ── User-facing: submit report ────────────────────────────────────────────

    /**
     * Submit a report from a regular user against a post.
     *
     * @return {@code true} if the report was accepted, {@code false} if the
     *         user has already reported this post.
     */
    public boolean submitReport(UUID postId, UUID reporterId,
                                UUID reportedAuthorId, String reason) {
        if (postId == null || reporterId == null || reportedAuthorId == null) return false;
        String clean = reason == null ? "" : reason.trim();
        return PostReportRepository.getInstance()
                .createReport(postId, reporterId, reportedAuthorId, clean) != null;
    }

    // ── Admin-facing: read ────────────────────────────────────────────────────

    /** @return all currently pending post reports, newest first. */
    public List<PostReport> getPendingReports() {
        return PostReportRepository.getInstance().getPendingReports();
    }

    // ── Admin-facing: resolve ─────────────────────────────────────────────────

    /**
     * Dismiss a report: mark DISMISSED, take no further action.
     * The reported post remains visible in the feed.
     */
    public void dismissReport(String reportId, UUID adminId) {
        PostReportRepository.getInstance()
                .updateStatus(reportId, PostReportStatus.DISMISSED, adminId);
    }

    /**
     * Delete the post referenced by a report.
     * <ul>
     *   <li>Marks the post as deleted in {@link PostDAO} — it disappears from the feed.</li>
     *   <li>Removes it from the hashtag index.</li>
     *   <li>Marks all reports for this post as {@link PostReportStatus#POST_DELETED}.</li>
     * </ul>
     */
    public void deleteReportedPost(String reportId, UUID adminId) {
        PostReport report = PostReportRepository.getInstance().findById(reportId);
        if (report == null) return;

        // Delete the post in PostDAO — it will no longer appear in the feed.
        Post post = PostDAO.getInstance().get(new Post(report.getPostId()));
        if (post != null) {
            post.setDeleted(true);
            HashtagService.getInstance().removePost(post);
        }

        // Resolve every pending report for this post (not just the tapped one).
        for (PostReport r : PostReportRepository.getInstance().getReportsForPost(report.getPostId())) {
            if (r.getStatus() == PostReportStatus.PENDING) {
                PostReportRepository.getInstance()
                        .updateStatus(r.getReportId(), PostReportStatus.POST_DELETED, adminId);
            }
        }
    }

    /**
     * Ban the author of the reported post.
     * <ul>
     *   <li>Adds the author to {@link BanRepository} — they cannot create new posts.</li>
     *   <li>Existing posts by the author stay visible.</li>
     *   <li>Marks the triggering report as {@link PostReportStatus#AUTHOR_BANNED}.</li>
     * </ul>
     */
    public void banReportedAuthor(String reportId, UUID adminId) {
        PostReport report = PostReportRepository.getInstance().findById(reportId);
        if (report == null) return;

        BanRepository.getInstance().ban(report.getReportedAuthorId());
        PostReportRepository.getInstance()
                .updateStatus(reportId, PostReportStatus.AUTHOR_BANNED, adminId);
    }
}

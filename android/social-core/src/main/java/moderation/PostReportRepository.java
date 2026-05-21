package moderation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton in-memory store for post-level reports.
 *
 * Design principles:
 *  - Repository pattern: all report I/O goes through this class.
 *  - Encapsulation: internal map is never exposed; callers receive copies.
 *  - Single Responsibility: only manages post-report storage and queries.
 *  - Separation of concerns: no UI or Activity logic here.
 *
 * Message.java is not modified — reports are post-level, not message-level.
 *
 * Persistence: in-memory only for this session. Survives navigation but not
 * app process restart (matches the rest of the prototype's runtime state).
 */
public final class PostReportRepository {

    private static PostReportRepository instance;

    /** reportId → PostReport */
    private final Map<String, PostReport> reports = new HashMap<>();

    /** postId → set of reporterIds who already submitted (prevent duplicates) */
    private final Map<UUID, List<UUID>> reportersByPost = new HashMap<>();

    private PostReportRepository() {}

    public static PostReportRepository getInstance() {
        if (instance == null) instance = new PostReportRepository();
        return instance;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Create a new PENDING report.
     *
     * @return the created report, or {@code null} if this user already
     *         reported this post.
     */
    public PostReport createReport(UUID postId, UUID reporterId,
                                   UUID reportedAuthorId, String reason) {
        if (hasReported(postId, reporterId)) return null;

        String reportId = UUID.randomUUID().toString();
        PostReport report = new PostReport(reportId, postId, reporterId,
                reportedAuthorId, reason);
        reports.put(reportId, report);
        reportersByPost.computeIfAbsent(postId, k -> new ArrayList<>()).add(reporterId);
        return report;
    }

    /**
     * Move a report to a resolved state. Called only by
     * {@link moderation.AdminModerationService}.
     */
    public void updateStatus(String reportId, PostReportStatus status, UUID adminId) {
        PostReport r = reports.get(reportId);
        if (r != null) r.resolve(status, adminId);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** @return unmodifiable list of all PENDING reports, newest first. */
    public List<PostReport> getPendingReports() {
        List<PostReport> pending = new ArrayList<>();
        for (PostReport r : reports.values()) {
            if (r.getStatus() == PostReportStatus.PENDING) pending.add(r);
        }
        pending.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return Collections.unmodifiableList(pending);
    }

    /** @return unmodifiable list of all reports, regardless of status. */
    public List<PostReport> getAllReports() {
        return Collections.unmodifiableList(new ArrayList<>(reports.values()));
    }

    /** @return unmodifiable list of all reports for a given post. */
    public List<PostReport> getReportsForPost(UUID postId) {
        List<PostReport> result = new ArrayList<>();
        for (PostReport r : reports.values()) {
            if (postId.equals(r.getPostId())) result.add(r);
        }
        return Collections.unmodifiableList(result);
    }

    /** Look up one report by its ID. */
    public PostReport findById(String reportId) {
        return reports.get(reportId);
    }

    /** Whether {@code reporterId} has already reported {@code postId}. */
    public boolean hasReported(UUID postId, UUID reporterId) {
        List<UUID> reporters = reportersByPost.get(postId);
        return reporters != null && reporters.contains(reporterId);
    }

    /** Total report submissions for a post (including resolved ones). */
    public int getReportCountForPost(UUID postId) {
        return getReportsForPost(postId).size();
    }

    /** Reset — used in tests. */
    public void clear() {
        reports.clear();
        reportersByPost.clear();
    }
}

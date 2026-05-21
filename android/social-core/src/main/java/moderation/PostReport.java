package moderation;

import java.util.UUID;

/**
 * Immutable identity fields of a user-submitted post report, with mutable
 * status updated only by {@link PostReportRepository}.
 *
 * Kept entirely separate from {@link dao.model.Message} — this is post-level
 * admin workflow state, not per-message state. Message.java is not modified.
 *
 * Demonstrates encapsulation: status mutations are package-private so only
 * {@link PostReportRepository} can change them.
 */
public final class PostReport {

    private final String reportId;
    private final UUID postId;
    private final UUID reporterId;
    private final UUID reportedAuthorId;
    private final String reason;
    private final long createdAt;

    // Mutable admin-resolution fields — write access restricted to package
    private PostReportStatus status;
    private long resolvedAt;
    private UUID resolvedByAdminId;

    PostReport(String reportId, UUID postId, UUID reporterId,
               UUID reportedAuthorId, String reason) {
        this.reportId          = reportId;
        this.postId            = postId;
        this.reporterId        = reporterId;
        this.reportedAuthorId  = reportedAuthorId;
        this.reason            = reason;
        this.status            = PostReportStatus.PENDING;
        this.createdAt         = System.currentTimeMillis();
        this.resolvedAt        = 0;
        this.resolvedByAdminId = null;
    }

    // ── Public getters (encapsulated — no mutable internals exposed) ──────────

    public String          getReportId()         { return reportId; }
    public UUID            getPostId()            { return postId; }
    public UUID            getReporterId()        { return reporterId; }
    public UUID            getReportedAuthorId()  { return reportedAuthorId; }
    public String          getReason()            { return reason; }
    public PostReportStatus getStatus()           { return status; }
    public long            getCreatedAt()         { return createdAt; }
    public long            getResolvedAt()        { return resolvedAt; }
    public UUID            getResolvedByAdminId() { return resolvedByAdminId; }

    // ── Package-private mutation — only PostReportRepository may call this ────

    void resolve(PostReportStatus newStatus, UUID adminId) {
        this.status            = newStatus;
        this.resolvedAt        = System.currentTimeMillis();
        this.resolvedByAdminId = adminId;
    }
}

package moderation;

/**
 * Lifecycle states of a post-level report.
 * Lives separately from Message state — this is post-level/admin workflow state only.
 */
public enum PostReportStatus {
    /** The report has been submitted and not yet acted on. */
    PENDING,
    /** Admin reviewed and decided no action is needed. Post stays visible. */
    DISMISSED,
    /** Admin deleted the reported post; post is removed from the feed. */
    POST_DELETED,
    /** Admin banned the reported post's author; author cannot create new posts. */
    AUTHOR_BANNED
}

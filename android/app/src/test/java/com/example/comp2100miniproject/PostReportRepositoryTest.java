package com.example.comp2100miniproject;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import moderation.BanRepository;
import moderation.PostReport;
import moderation.PostReportRepository;
import moderation.PostReportStatus;

import static org.junit.Assert.*;

/**
 * Unit tests for PostReportRepository and BanRepository.
 * Message.java is not involved — this tests post-level moderation state only.
 */
public class PostReportRepositoryTest {

    private final UUID postId    = UUID.randomUUID();
    private final UUID reporter  = UUID.randomUUID();
    private final UUID author    = UUID.randomUUID();
    private final UUID adminId   = UUID.randomUUID();

    @Before
    public void setUp() {
        PostReportRepository.getInstance().clear();
        BanRepository.getInstance().clear();
    }

    // ── Repository basics ─────────────────────────────────────────────────────

    @Test
    public void createReportReturnsPendingReport() {
        PostReport r = PostReportRepository.getInstance()
                .createReport(postId, reporter, author, "spam");
        assertNotNull(r);
        assertEquals(PostReportStatus.PENDING, r.getStatus());
        assertEquals(postId, r.getPostId());
        assertEquals(reporter, r.getReporterId());
        assertEquals(author, r.getReportedAuthorId());
        assertEquals("spam", r.getReason());
    }

    @Test
    public void duplicateReportIsRejected() {
        PostReportRepository.getInstance().createReport(postId, reporter, author, "spam");
        PostReport duplicate = PostReportRepository.getInstance()
                .createReport(postId, reporter, author, "again");
        assertNull("Second report from same user should be rejected", duplicate);
    }

    @Test
    public void differentUsersCanReportSamePost() {
        UUID reporter2 = UUID.randomUUID();
        PostReport r1 = PostReportRepository.getInstance().createReport(postId, reporter, author, "spam");
        PostReport r2 = PostReportRepository.getInstance().createReport(postId, reporter2, author, "abuse");
        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(2, PostReportRepository.getInstance().getPendingReports().size());
    }

    @Test
    public void hasReportedReturnsTrueAfterSubmission() {
        PostReportRepository.getInstance().createReport(postId, reporter, author, "x");
        assertTrue(PostReportRepository.getInstance().hasReported(postId, reporter));
    }

    @Test
    public void hasReportedReturnsFalseForNewUser() {
        assertFalse(PostReportRepository.getInstance().hasReported(postId, UUID.randomUUID()));
    }

    @Test
    public void dismissedReportLeavesNoNewPending() {
        PostReport r = PostReportRepository.getInstance()
                .createReport(postId, reporter, author, "reason");
        PostReportRepository.getInstance()
                .updateStatus(r.getReportId(), PostReportStatus.DISMISSED, adminId);
        assertTrue(PostReportRepository.getInstance().getPendingReports().isEmpty());
        assertEquals(PostReportStatus.DISMISSED, r.getStatus());
    }

    @Test
    public void findByIdReturnsCorrectReport() {
        PostReport r = PostReportRepository.getInstance()
                .createReport(postId, reporter, author, "y");
        assertNotNull(r);
        PostReport found = PostReportRepository.getInstance().findById(r.getReportId());
        assertEquals(r.getReportId(), found.getReportId());
    }

    @Test
    public void pendingListIsNewestFirst() throws InterruptedException {
        UUID post2 = UUID.randomUUID();
        PostReport r1 = PostReportRepository.getInstance()
                .createReport(postId, reporter, author, "first");
        Thread.sleep(5); // ensure timestamp difference
        PostReport r2 = PostReportRepository.getInstance()
                .createReport(post2, UUID.randomUUID(), author, "second");
        List<PostReport> pending = PostReportRepository.getInstance().getPendingReports();
        assertEquals(r2.getReportId(), pending.get(0).getReportId());
    }

    @Test
    public void clearResetsAllState() {
        PostReportRepository.getInstance().createReport(postId, reporter, author, "x");
        PostReportRepository.getInstance().clear();
        assertTrue(PostReportRepository.getInstance().getPendingReports().isEmpty());
        assertFalse(PostReportRepository.getInstance().hasReported(postId, reporter));
    }

    // ── BanRepository ─────────────────────────────────────────────────────────

    @Test
    public void bannedUserIsBanned() {
        UUID userId = UUID.randomUUID();
        assertFalse(BanRepository.getInstance().isBanned(userId));
        BanRepository.getInstance().ban(userId);
        assertTrue(BanRepository.getInstance().isBanned(userId));
    }

    @Test
    public void unbanRemovesBan() {
        UUID userId = UUID.randomUUID();
        BanRepository.getInstance().ban(userId);
        BanRepository.getInstance().unban(userId);
        assertFalse(BanRepository.getInstance().isBanned(userId));
    }

    @Test
    public void banIsIdempotent() {
        UUID userId = UUID.randomUUID();
        BanRepository.getInstance().ban(userId);
        BanRepository.getInstance().ban(userId);
        assertTrue(BanRepository.getInstance().isBanned(userId));
    }

    @Test
    public void nullUserIdIsNotBanned() {
        assertFalse(BanRepository.getInstance().isBanned(null));
    }
}

package com.example.comp2100miniproject;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import postview.PostViewService;

import static org.junit.Assert.*;

/**
 * Unit tests for PostViewService — the post-level view-count tracker.
 * Message.java is not involved: view counts are post-level, not message-level.
 */
public class PostViewServiceTest {

    @Before
    public void reset() {
        PostViewService.getInstance().clear();
    }

    @Test
    public void newPostStartsAtZeroViews() {
        UUID id = UUID.randomUUID();
        assertEquals(0, PostViewService.getInstance().getViewCount(id));
    }

    @Test
    public void firstOpenGivesCountOfOne() {
        UUID id = UUID.randomUUID();
        PostViewService.getInstance().recordView(id);
        assertEquals(1, PostViewService.getInstance().getViewCount(id));
    }

    @Test
    public void eachOpenIncrementsCountByOne() {
        UUID id = UUID.randomUUID();
        PostViewService.getInstance().recordView(id);
        PostViewService.getInstance().recordView(id);
        PostViewService.getInstance().recordView(id);
        assertEquals(3, PostViewService.getInstance().getViewCount(id));
    }

    @Test
    public void differentPostsTrackedIndependently() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        PostViewService.getInstance().recordView(id1);
        PostViewService.getInstance().recordView(id1);
        PostViewService.getInstance().recordView(id2);

        assertEquals(2, PostViewService.getInstance().getViewCount(id1));
        assertEquals(1, PostViewService.getInstance().getViewCount(id2));
    }

    @Test
    public void clearResetsAllCounts() {
        UUID id = UUID.randomUUID();
        PostViewService.getInstance().recordView(id);
        PostViewService.getInstance().recordView(id);
        assertEquals(2, PostViewService.getInstance().getViewCount(id));

        PostViewService.getInstance().clear();
        assertEquals(0, PostViewService.getInstance().getViewCount(id));
    }

    @Test
    public void unviewedPostCountIsAlwaysZero() {
        // No recordView calls — count must never be negative or non-zero.
        for (int i = 0; i < 5; i++) {
            assertEquals(0, PostViewService.getInstance().getViewCount(UUID.randomUUID()));
        }
    }
}

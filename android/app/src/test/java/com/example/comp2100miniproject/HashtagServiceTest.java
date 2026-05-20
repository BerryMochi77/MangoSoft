package com.example.comp2100miniproject;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import dao.PostDAO;
import dao.model.Post;
import hashtag.HashtagParser;
import hashtag.HashtagService;
import hashtag.TagCount;

/**
 * Unit tests for HashtagService — index management, search, and trending.
 * Each test resets the PostDAO and HashtagService to a clean state.
 */
public class HashtagServiceTest {

    @Before
    public void setUp() {
        PostDAO.getInstance().clear();
        HashtagService.getInstance().clear();
    }

    private Post makePost(String topic) {
        Post post = new Post(UUID.randomUUID(), UUID.randomUUID(), topic);
        post.setHashtags(HashtagParser.extract(topic));
        PostDAO.getInstance().add(post);
        HashtagService.getInstance().indexPost(post);
        return post;
    }

    @Test
    public void searchByTagFindsMatchingPosts() {
        Post a = makePost("This is spam #spam");
        Post b = makePost("Need help here #help");
        makePost("No hashtag post");

        List<Post> results = HashtagService.getInstance().searchByTag("spam");
        assertEquals(1, results.size());
        assertEquals(a.id, results.get(0).id);
    }

    @Test
    public void searchIsCaseInsensitive() {
        Post a = makePost("Post A #spam");
        Post c = makePost("Post C #Spam");

        // Both posts have normalized tag "spam"
        List<Post> results = HashtagService.getInstance().searchByTag("SPAM");
        assertEquals(2, results.size());
    }

    @Test
    public void searchStripsLeadingHash() {
        Post a = makePost("Help needed #help");
        List<Post> results = HashtagService.getInstance().searchByTag("#help");
        assertEquals(1, results.size());
        assertEquals(a.id, results.get(0).id);
    }

    @Test
    public void searchReturnsEmptyForUnknownTag() {
        makePost("Some post #spam");
        List<Post> results = HashtagService.getInstance().searchByTag("nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    public void trendingOrderedByCountDescending() {
        makePost("#spam post 1");
        makePost("#spam post 2");
        makePost("#spam post 3");
        makePost("#help post 1");
        makePost("#help post 2");
        makePost("#bug post 1");

        List<TagCount> trending = HashtagService.getInstance().getTrending();
        assertFalse(trending.isEmpty());
        assertEquals("spam", trending.get(0).getTag());
        assertEquals(3, trending.get(0).getCount());
        assertEquals("help", trending.get(1).getTag());
        assertEquals(2, trending.get(1).getCount());
        assertEquals("bug", trending.get(2).getTag());
        assertEquals(1, trending.get(2).getCount());
    }

    @Test
    public void removePostUpdatesIndex() {
        Post post = makePost("Post to delete #spam");
        assertEquals(1, HashtagService.getInstance().searchByTag("spam").size());

        HashtagService.getInstance().removePost(post);
        assertTrue(HashtagService.getInstance().searchByTag("spam").isEmpty());
    }

    @Test
    public void deletedPostExcludedFromSearch() {
        Post post = makePost("Deleted post #spam");
        post.setDeleted(true);

        List<Post> results = HashtagService.getInstance().searchByTag("spam");
        assertTrue(results.isEmpty());
    }

    @Test
    public void postWithNoHashtagsWorksNormally() {
        Post post = makePost("A post with no hashtags");
        assertTrue(post.getHashtags().isEmpty());
        List<Post> results = HashtagService.getInstance().searchByTag("anything");
        assertTrue(results.isEmpty());
        assertTrue(HashtagService.getInstance().getTrending().isEmpty());
    }

    @Test
    public void rebuildIndexFromPostDAO() {
        // Add posts directly to DAO without indexing
        Post post = new Post(UUID.randomUUID(), UUID.randomUUID(), "#spam topic");
        post.setHashtags(HashtagParser.extract("#spam topic"));
        PostDAO.getInstance().add(post);

        // Index is still empty; rebuild should fix it
        assertTrue(HashtagService.getInstance().searchByTag("spam").isEmpty());
        HashtagService.getInstance().rebuildIndex();
        assertEquals(1, HashtagService.getInstance().searchByTag("spam").size());
    }

    @Test
    public void trendingIsUnmodifiable() {
        makePost("#spam");
        List<TagCount> trending = HashtagService.getInstance().getTrending();
        assertThrows(UnsupportedOperationException.class, () -> trending.add(new TagCount("x", 1)));
    }

    @Test
    public void searchResultIsUnmodifiable() {
        makePost("#spam");
        List<Post> results = HashtagService.getInstance().searchByTag("spam");
        assertThrows(UnsupportedOperationException.class, () -> results.add(null));
    }
}

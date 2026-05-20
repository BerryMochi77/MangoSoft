package com.example.comp2100miniproject;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

import hashtag.HashtagParser;

/**
 * Unit tests for HashtagParser — extraction, normalization, and deduplication.
 */
public class HashtagParserTest {

    @Test
    public void extractBasicHashtags() {
        List<String> tags = HashtagParser.extract("Please check this #Spam #help #spam");
        assertEquals(2, tags.size());
        assertTrue(tags.contains("spam"));
        assertTrue(tags.contains("help"));
    }

    @Test
    public void normalizedToLowercase() {
        List<String> tags = HashtagParser.extract("#MODERATION #Moderation #moderation");
        assertEquals(1, tags.size());
        assertEquals("moderation", tags.get(0));
    }

    @Test
    public void deduplicatesTags() {
        List<String> tags = HashtagParser.extract("#spam #spam #SPAM #Spam");
        assertEquals(1, tags.size());
        assertEquals("spam", tags.get(0));
    }

    @Test
    public void emptyTextReturnsEmptyList() {
        assertTrue(HashtagParser.extract("").isEmpty());
        assertTrue(HashtagParser.extract(null).isEmpty());
        assertTrue(HashtagParser.extract("   ").isEmpty());
    }

    @Test
    public void noTagsReturnsEmptyList() {
        assertTrue(HashtagParser.extract("No tags here").isEmpty());
    }

    @Test
    public void supportsUnderscoresAndDigits() {
        List<String> tags = HashtagParser.extract("#bug_fix #issue123 #HELLO_WORLD");
        assertEquals(3, tags.size());
        assertTrue(tags.contains("bug_fix"));
        assertTrue(tags.contains("issue123"));
        assertTrue(tags.contains("hello_world"));
    }

    @Test
    public void preservesInsertionOrderForFirstOccurrence() {
        List<String> tags = HashtagParser.extract("#spam #help #SPAM");
        assertEquals(0, tags.indexOf("spam"));
        assertEquals(1, tags.indexOf("help"));
    }

    @Test
    public void multipleTagsWithContent() {
        List<String> tags = HashtagParser.extract("This looks like spam #spam #moderation please review");
        assertEquals(2, tags.size());
        assertTrue(tags.contains("spam"));
        assertTrue(tags.contains("moderation"));
    }

    @Test
    public void returnedListIsMutable() {
        List<String> tags = HashtagParser.extract("#spam");
        tags.add("extra");
        assertEquals(2, tags.size());
    }
}

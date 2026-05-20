package hashtag.search;

import dao.model.Post;

import java.util.List;

/**
 * Strategy interface for post search.
 *
 * Demonstrates the Strategy design pattern: different search algorithms
 * (hashtag lookup, keyword scan, etc.) can be swapped at runtime without
 * modifying Activity code. New strategies can be added without touching
 * existing search code.
 */
public interface PostSearchStrategy {

    /**
     * Execute the search and return matching non-deleted posts.
     *
     * @param query the search term (e.g. "spam", "#help", "Android")
     * @return unmodifiable list of matching posts
     */
    List<Post> search(String query);
}

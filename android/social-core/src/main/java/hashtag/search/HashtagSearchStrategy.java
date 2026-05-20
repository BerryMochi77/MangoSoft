package hashtag.search;

import dao.model.Post;
import hashtag.HashtagService;

import java.util.List;

/**
 * Searches posts by hashtag using the in-memory HashtagService index.
 * O(1) lookup via the inverted index, then a linear pass to build the result list.
 */
public class HashtagSearchStrategy implements PostSearchStrategy {

    private final HashtagService service = HashtagService.getInstance();

    @Override
    public List<Post> search(String query) {
        return service.searchByTag(query);
    }
}

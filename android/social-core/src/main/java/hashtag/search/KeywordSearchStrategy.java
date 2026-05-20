package hashtag.search;

import dao.PostDAO;
import dao.model.Post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Searches posts whose topic contains the query as a case-insensitive substring.
 * Linear scan over all posts — complements the hashtag-based index strategy.
 */
public class KeywordSearchStrategy implements PostSearchStrategy {

    @Override
    public List<Post> search(String query) {
        String lower = query.toLowerCase().trim();
        List<Post> result = new ArrayList<>();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post post = it.next();
            if (!post.isDeleted() && post.topic.toLowerCase().contains(lower)) {
                result.add(post);
            }
        }
        return Collections.unmodifiableList(result);
    }
}

package hashtag;

import dao.PostDAO;
import dao.model.Post;

import java.util.*;

/**
 * Singleton service maintaining an in-memory inverted index: hashtag → set of post UUIDs.
 *
 * Design principles demonstrated:
 * - Separation of concerns: hashtag business logic lives here, not in Activity or DAO code.
 * - Encapsulation: the internal map is never exposed; callers receive defensive copies.
 * - Single Responsibility: this service only manages the hashtag index and queries.
 *
 * Both regular users (content discovery) and moderators (#spam, #abuse, #harassment)
 * share this service — access is not role-restricted by design.
 */
public final class HashtagService {

    private static HashtagService instance;

    /** Inverted index: normalized tag → set of post UUIDs containing that tag. */
    private final Map<String, Set<UUID>> tagIndex = new HashMap<>();

    private HashtagService() {}

    public static HashtagService getInstance() {
        if (instance == null) instance = new HashtagService();
        return instance;
    }

    /**
     * Rebuild the index from scratch using PostDAO.
     * Must be called after DataManager.readAll() loads all posts.
     */
    public void rebuildIndex() {
        tagIndex.clear();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post post = it.next();
            if (!post.isDeleted()) indexPost(post);
        }
    }

    /** Add a single post's hashtags to the index (called on create or load). */
    public void indexPost(Post post) {
        for (String tag : post.getHashtags()) {
            tagIndex.computeIfAbsent(tag, k -> new HashSet<>()).add(post.id);
        }
    }

    /** Remove a post from the index (called when a post is deleted). */
    public void removePost(Post post) {
        for (String tag : post.getHashtags()) {
            Set<UUID> ids = tagIndex.get(tag);
            if (ids != null) {
                ids.remove(post.id);
                if (ids.isEmpty()) tagIndex.remove(tag);
            }
        }
    }

    /**
     * Find all non-deleted posts tagged with {@code rawTag}.
     * Input is case-insensitive; leading '#' is stripped automatically.
     *
     * @return an unmodifiable list of matching posts
     */
    public List<Post> searchByTag(String rawTag) {
        String normalized = rawTag.toLowerCase().replaceAll("^#", "").trim();
        Set<UUID> ids = tagIndex.getOrDefault(normalized, Collections.emptySet());

        List<Post> result = new ArrayList<>();
        Iterator<Post> it = PostDAO.getInstance().getAll();
        while (it.hasNext()) {
            Post post = it.next();
            if (!post.isDeleted() && ids.contains(post.id)) result.add(post);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Return all known tags sorted by post count (highest first), then alphabetically.
     *
     * @return an unmodifiable list of TagCount snapshots
     */
    public List<TagCount> getTrending() {
        List<TagCount> counts = new ArrayList<>();
        for (Map.Entry<String, Set<UUID>> entry : tagIndex.entrySet()) {
            counts.add(new TagCount(entry.getKey(), entry.getValue().size()));
        }
        counts.sort(Comparator.comparingInt(TagCount::getCount).reversed()
                .thenComparing(TagCount::getTag));
        return Collections.unmodifiableList(counts);
    }

    /** Reset index — called before DataManager reloads all data. */
    public void clear() {
        tagIndex.clear();
    }
}

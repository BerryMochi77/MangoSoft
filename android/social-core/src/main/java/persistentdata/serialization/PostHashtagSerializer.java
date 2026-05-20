package persistentdata.serialization;

import dao.model.PostHashtagEntry;

import java.util.UUID;

/**
 * Serializes each (postId, tag) association as a 2-column CSV row.
 * One row per hashtag per post — follows the same pattern as HiddenMessageSerializer.
 */
public class PostHashtagSerializer implements Serializer<PostHashtagEntry, String[]> {

    @Override
    public String[] serialize(PostHashtagEntry entry) {
        return new String[]{ entry.postId().toString(), entry.tag() };
    }

    @Override
    public PostHashtagEntry deserialize(String[] data) {
        return new PostHashtagEntry(UUID.fromString(data[0]), data[1]);
    }
}

package hashtag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and normalizes hashtags from free text.
 * Tags support letters, numbers, and underscores. Results are lowercased and deduplicated.
 *
 * Used by both regular users (for content discovery) and moderators
 * (to quickly find posts tagged #spam, #harassment, #abuse, etc.).
 *
 * Separation of concerns: all parsing logic lives here, not in Activities or DAOs.
 */
public final class HashtagParser {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)");

    private HashtagParser() {}

    /**
     * Extract unique, normalized hashtags from text.
     * Example: "#Spam #help #SPAM" → ["spam", "help"]
     * Example: "No tags here" → []
     *
     * @param text the raw post content
     * @return a new mutable list of lowercase, deduplicated tag strings (without leading '#')
     */
    public static List<String> extract(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        Matcher m = HASHTAG_PATTERN.matcher(text);
        while (m.find()) {
            seen.add(m.group(1).toLowerCase());
        }
        return new ArrayList<>(seen);
    }

    /**
     * Strip every {@code #tag} occurrence from {@code text} and collapse the
     * remaining whitespace, so the same string can be rendered as a clean
     * title with the tags shown separately as chips.
     * Example: "Debugging RecyclerView adapters #android #bug" → "Debugging RecyclerView adapters"
     * The original text is left untouched if no tags are present.
     *
     * @param text the raw post title or body
     * @return the text with all hashtags removed and whitespace tidied
     */
    public static String stripTags(String text) {
        if (text == null || text.isEmpty()) return "";
        String stripped = HASHTAG_PATTERN.matcher(text).replaceAll("");
        // Collapse runs of whitespace introduced by the removals.
        return stripped.replaceAll("\\s+", " ").trim();
    }
}

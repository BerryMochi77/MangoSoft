package hashtag;

/** Immutable snapshot of how many posts contain a given hashtag. Used for trending display. */
public final class TagCount {

    private final String tag;
    private final int count;

    public TagCount(String tag, int count) {
        this.tag = tag;
        this.count = count;
    }

    public String getTag()  { return tag; }
    public int   getCount() { return count; }
}

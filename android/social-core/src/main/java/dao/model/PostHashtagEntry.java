package dao.model;

import java.util.UUID;

/** Represents a single post-tag association used for CSV persistence. */
public record PostHashtagEntry(UUID postId, String tag) {}

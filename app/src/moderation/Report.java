package moderation;

import java.util.UUID;


public record Report(UUID message, UUID user, long timestamp) {}

package moderation;

import java.util.UUID;


public record Report(UUID message, UUID user, long timestamp, String reason) {
	public Report(UUID message, UUID user, long timestamp) {
		this(message, user, timestamp, "");
	}
}

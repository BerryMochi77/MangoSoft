package dao.model;

import java.util.UUID;
import java.util.Objects;

public class Message {
	private final UUID id;
	private final UUID poster;
	private final UUID thread;
	private final long timestamp;
	private final String message;
	private boolean hidden;

	public Message(UUID id, UUID poster, UUID thread, long timestamp, String message) {
		this(id, poster, thread, timestamp, message, false);
	}

	public Message(UUID id, UUID poster, UUID thread, long timestamp, String message, boolean hidden) {
		this.id = id;
		this.poster = poster;
		this.thread = thread;
		this.timestamp = timestamp;
		this.message = message;
		this.hidden = hidden;
	}

	public UUID id() {
		return id;
	}

	public UUID poster() {
		return poster;
	}

	public UUID thread() {
		return thread;
	}

	public long timestamp() {
		return timestamp;
	}

	public String message() {
		return message;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof Message other)) return false;
		return timestamp == other.timestamp
				&& hidden == other.hidden
				&& Objects.equals(id, other.id)
				&& Objects.equals(poster, other.poster)
				&& Objects.equals(thread, other.thread)
				&& Objects.equals(message, other.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, poster, thread, timestamp, message, hidden);
	}

	@Override
	public String toString() {
		return "Message[id=%s, poster=%s, thread=%s, timestamp=%s, message=%s, hidden=%s]"
				.formatted(id, poster, thread, timestamp, message, hidden);
	}
}

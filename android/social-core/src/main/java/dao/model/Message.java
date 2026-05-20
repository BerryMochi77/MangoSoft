package dao.model;

import java.util.UUID;
import java.util.Objects;

public class Message {
	private final UUID id;
	private final UUID poster;
	private final UUID thread;
	private final long timestamp;
	private String message;
	private boolean hidden;
	private boolean edited;
	private boolean deleted;

	public Message(UUID id, UUID poster, UUID thread, long timestamp, String message) {
		this(id, poster, thread, timestamp, message, false);
	}

	public Message(UUID id, UUID poster, UUID thread, long timestamp, String message, boolean hidden) {
		this(id, poster, thread, timestamp, message, hidden, false, false);
	}

	public Message(UUID id, UUID poster, UUID thread, long timestamp, String message, boolean hidden, boolean edited, boolean deleted) {
		this.id = id;
		this.poster = poster;
		this.thread = thread;
		this.timestamp = timestamp;
		this.message = message;
		this.hidden = hidden;
		this.edited = edited;
		this.deleted = deleted;
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

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(boolean edited) {
		this.edited = edited;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof Message other)) return false;
		return timestamp == other.timestamp
				&& hidden == other.hidden
				&& edited == other.edited
				&& deleted == other.deleted
				&& Objects.equals(id, other.id)
				&& Objects.equals(poster, other.poster)
				&& Objects.equals(thread, other.thread)
				&& Objects.equals(message, other.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, poster, thread, timestamp, message, hidden, edited, deleted);
	}

	@Override
	public String toString() {
		return "Message[id=%s, poster=%s, thread=%s, timestamp=%s, message=%s, hidden=%s, edited=%s, deleted=%s]"
				.formatted(id, poster, thread, timestamp, message, hidden, edited, deleted);
	}
}

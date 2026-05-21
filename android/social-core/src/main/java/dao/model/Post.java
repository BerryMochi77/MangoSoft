package dao.model;

import dao.MessageComparator;
import messagestate.MessageDeletionRegistry;
import sorteddata.SortedData;
import sorteddata.SortedDataFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Post implements HasUUID {
	public final UUID id;
	public final UUID poster;
	public String topic;
    public final SortedData<Message> messages;
	private boolean edited;
	private boolean deleted;
	private List<String> hashtags;
	private String body;
	/** Epoch-millisecond timestamp of when this post was created. */
	private long createdAt;

	public Post(UUID id, UUID poster, String topic) {
		this.id = id;
		this.poster = poster;
		this.topic = topic;
		this.messages = SortedDataFactory.makeSortedData(MessageComparator.getInstance());
		this.hashtags = new ArrayList<>();
		this.body = "";
		this.createdAt = System.currentTimeMillis();
	}

	public Post(UUID id, UUID poster, String topic, boolean edited, boolean deleted) {
		this(id, poster, topic);
		this.edited = edited;
		this.deleted = deleted;
	}

	public Post(UUID id, UUID poster, String topic, boolean edited, boolean deleted, long createdAt) {
		this(id, poster, topic, edited, deleted);
		setCreatedAt(createdAt);
	}

	public Post(UUID id) {
		this(id, null, null);
	}

	public SortedData<Message> getVisibleMessages(boolean isAdmin) {
		SortedData<Message> visibleMessages = SortedDataFactory.makeSortedData(MessageComparator.getInstance());

		MessageDeletionRegistry deletions = MessageDeletionRegistry.getInstance();
		for (var iterator = messages.getAll(); iterator.hasNext(); ) {
			Message message = iterator.next();
			boolean deleted = deletions.isDeleted(message.id());
			if (!deleted && (isAdmin || !message.isHidden())) visibleMessages.insert(message);
		}

		return visibleMessages;
	}

	public UUID getUUID() { return id; }

	/** Returns the epoch-ms timestamp when this post was created (in-memory). */
	public long getCreatedAt() { return createdAt; }

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
	}

	/** Returns an unmodifiable view of this post's hashtags (no leading '#', all lowercase). */
	public List<String> getHashtags() {
		return Collections.unmodifiableList(hashtags);
	}

	/** Replace the full hashtag list (defensive copy taken). */
	public void setHashtags(List<String> hashtags) {
		this.hashtags = new ArrayList<>(hashtags);
	}

	/** Add a single tag if not already present. */
	public void addHashtag(String tag) {
		if (!hashtags.contains(tag)) hashtags.add(tag);
	}

	/** Returns the post body/content (may be empty). */
	public String getBody() {
		return body == null ? "" : body;
	}

	/** Sets the post body/content. */
	public void setBody(String body) {
		this.body = body == null ? "" : body;
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
	
}

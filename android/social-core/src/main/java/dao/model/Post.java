package dao.model;

import dao.MessageComparator;
import sorteddata.SortedData;
import sorteddata.SortedDataFactory;

import java.util.UUID;
public class Post implements HasUUID {
	public final UUID id;
	public final UUID poster;
	public String topic;
    public final SortedData<Message> messages;
	private boolean edited;
	private boolean deleted;

	public Post(UUID id, UUID poster, String topic) {
		this.id = id;
		this.poster = poster;
		this.topic = topic;
		this.messages = SortedDataFactory.makeSortedData(MessageComparator.getInstance());
	}

	public Post(UUID id, UUID poster, String topic, boolean edited, boolean deleted) {
		this(id, poster, topic);
		this.edited = edited;
		this.deleted = deleted;
	}

	public Post(UUID id) {
		this(id, null, null);
	}

	public SortedData<Message> getVisibleMessages(boolean isAdmin) {
		SortedData<Message> visibleMessages = SortedDataFactory.makeSortedData(MessageComparator.getInstance());

		for (var iterator = messages.getAll(); iterator.hasNext(); ) {
			Message message = iterator.next();
			if (!message.isDeleted() && (isAdmin || !message.isHidden())) visibleMessages.insert(message);
		}

		return visibleMessages;
	}

	public UUID getUUID() { return id; }

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

package dao.model;

import dao.MessageComparator;
import sorteddata.SortedData;
import sorteddata.SortedDataFactory;

import java.util.UUID;

public class Post implements HasUUID {
	public final UUID id;
	public final UUID poster;
	public final String topic;
    public final SortedData<Message> messages;

	public Post(UUID id, UUID poster, String topic) {
		this.id = id;
		this.poster = poster;
		this.topic = topic;
		this.messages = SortedDataFactory.makeSortedData(MessageComparator.getInstance());
	}

	public Post(UUID id) {
		this(id, null, null);
	}

	public SortedData<Message> getVisibleMessages(boolean isAdmin) {
		SortedData<Message> visibleMessages = SortedDataFactory.makeSortedData(MessageComparator.getInstance());

		for (var iterator = messages.getAll(); iterator.hasNext(); ) {
			Message message = iterator.next();
			if (isAdmin || !message.isHidden()) visibleMessages.insert(message);
		}

		return visibleMessages;
	}

	public UUID getUUID() { return id; }
}

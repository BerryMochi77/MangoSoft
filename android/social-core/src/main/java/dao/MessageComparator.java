package dao;

import dao.model.Message;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {
	private static MessageComparator instance;

	/**
	 * Implements the Singleton design pattern. Because creating multiple instances
	 * of the MessageComparator would simply waste memory, we may use the Singleton
	 * design pattern to ensure only a single instance exists.
	 * @return the instance
	 */
	public static MessageComparator getInstance() {
		if (instance == null) instance = new MessageComparator();
		return instance;
	}
	private MessageComparator() {}

	/**
	 * Checks if two Messages are identical in these fields: timestamp, thread, poster, and ID.
	 * @param o1 the first object to be compared.
	 * @param o2 the second object to be compared.
	 * @return the result of the comparison, in line with standard Java comparison rules
	 */
	@Override
	public int compare(Message o1, Message o2) {
		int delta = Long.compare(o1.timestamp(), o2.timestamp());
		if (delta != 0) return delta;

		delta = o1.thread().compareTo(o2.thread());
		if (delta != 0) return delta;

		delta = o1.poster().compareTo(o2.poster());
		if (delta != 0) return delta;

		delta = o1.id().compareTo(o2.id());
		return delta;
	}
}

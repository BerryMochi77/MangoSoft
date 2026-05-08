package moderation;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.User;

import java.util.Iterator;
import java.util.UUID;

public class ModerationTools {

	public static boolean addReport(UUID message, UUID user, long timestamp) {
		if (!messageExists(message) || !userExists(user)) return false;
		return ReportRegistry.getInstance().addReport(message, user, timestamp);
	}

	public static boolean removeReport(UUID message, UUID user, long timestamp) {
		if (!messageExists(message) || !userExists(user)) return false;
		return ReportRegistry.getInstance().removeReport(message, user);
	}

	public static boolean hasReported(UUID message, UUID user) {
		if (!messageExists(message) || !userExists(user)) return false;
		return ReportRegistry.getInstance().hasReported(message, user);
	}

	public static boolean setHidden(UUID message, UUID user, boolean hidden) {
		Message targetMessage = getMessageByUUID(message);
		User targetUser = user == null ? null : UserDAO.getInstance().getByUUID(user);

		if (targetMessage == null || targetUser == null || targetUser.role() != User.Role.Admin) return false;

		targetMessage.setHidden(hidden);
		return true;
	}

	public static Iterator<Message> getReportedMessages(String strategy, int amount) {
		return ReportedMessageIteratorFactory.create(
				strategy,
				amount,
				ReportRegistry.getInstance()
		);
	}

	private static boolean userExists(UUID user) {
		return user != null && UserDAO.getInstance().getByUUID(user) != null;
	}

	private static boolean messageExists(UUID message) {
		return getMessageByUUID(message) != null;
	}

	private static Message getMessageByUUID(UUID message) {
		if (message == null) return null;

		Iterator<Message> messages = PostDAO.getInstance().getAllMessages();
		while (messages.hasNext()) {
			Message nextMessage = messages.next();
			if (message.equals(nextMessage.id())) return nextMessage;
		}
		return null;
	}
}

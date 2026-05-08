package moderation;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.User;

import java.util.Iterator;
import java.util.UUID;

public class ModerationTools {
	/**
	 * 为指定消息添加一条用户举报。
	 *
	 * @param message 被举报消息的 UUID
	 * @param user 发起举报的用户 UUID
	 * @param timestamp 举报发生的时间戳
	 * @return 如果消息和用户都存在，且该用户此前没有举报过该消息，返回 true；否则返回 false
	 */
	public static boolean addReport(UUID message, UUID user, long timestamp) {
		if (!messageExists(message) || !userExists(user)) return false;
		return ReportRegistry.getInstance().addReport(message, user, timestamp);
	}
	
	/**
	 * 移除指定用户对指定消息的举报。
	 *
	 * @param message 被取消举报的消息 UUID
	 * @param user 取消举报的用户 UUID
	 * @param timestamp 保留参数；本任务删除举报时不需要使用时间戳
	 * @return 如果消息和用户都存在，且该用户当前正在举报该消息，返回 true；否则返回 false
	 */
	public static boolean removeReport(UUID message, UUID user, long timestamp) {
		if (!messageExists(message) || !userExists(user)) return false;
		return ReportRegistry.getInstance().removeReport(message, user);
	}
	
	/**
	 * 检查某个用户是否已经举报过某条消息。
	 *
	 * @param message 待检查消息的 UUID
	 * @param user 待检查用户的 UUID
	 * @return 如果消息和用户都存在，且该用户已经举报该消息，返回 true；否则返回 false
	 */
	public static boolean hasReported(UUID message, UUID user) {
		if (!messageExists(message) || !userExists(user)) return false;
		return ReportRegistry.getInstance().hasReported(message, user);
	}
	
	public static boolean setHidden(UUID message, UUID user, boolean hidden) {
		Message targetMessage = getMessage(message);
		User targetUser = getUser(user);

		if (targetMessage == null || targetUser == null || targetUser.role() != User.Role.Admin) return false;

		targetMessage.setHidden(hidden);
		return true;
	}
	
	public static Iterator<Message> getReportedMessages(String strategy, int amount) {
		// TODO: task 4
		return null;
	}

	/**
	 * 判断用户 UUID 是否对应一个已注册用户。
	 *
	 * @param user 待检查用户 UUID
	 * @return 用户存在时返回 true，否则返回 false
	 */
	private static boolean userExists(UUID user) {
		return getUser(user) != null;
	}

	/**
	 * 判断消息 UUID 是否对应一条已存在消息。
	 * 现有代码没有按 UUID 查询消息的 DAO，因此这里从所有 Post 的消息中遍历查找。
	 *
	 * @param message 待检查消息 UUID
	 * @return 消息存在时返回 true，否则返回 false
	 */
	private static boolean messageExists(UUID message) {
		return getMessage(message) != null;
	}

	private static User getUser(UUID user) {
		return user == null ? null : UserDAO.getInstance().getByUUID(user);
	}

	private static Message getMessage(UUID message) {
		if (message == null) return null;

		Iterator<Message> messages = PostDAO.getInstance().getAllMessages();
		while (messages.hasNext()) {
			Message next = messages.next();
			if (message.equals(next.id())) return next;
		}
		return null;
	}
}

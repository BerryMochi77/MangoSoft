package moderation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 举报记录的内存存储中心。
 * 使用双层 HashMap 保存数据：外层按消息 UUID 分组，内层按用户 UUID 分组。
 * 这样可以高效判断同一用户是否已经举报过同一条消息。
 */
class ReportRegistry {
	private static ReportRegistry instance;

	/**
	 * 获取举报存储中心的单例实例。
	 *
	 * @return 本次运行中共享的 ReportRegistry 实例
	 */
	static ReportRegistry getInstance() {
		if (instance == null) instance = new ReportRegistry();
		return instance;
	}

	/**
	 * 所有活跃举报记录。
	 * 外层 key 是消息 UUID，内层 key 是用户 UUID，value 是对应的举报详情。
	 */
	private final Map<UUID, Map<UUID, Report>> reportsByMessage = new HashMap<>();

	/**
	 * 添加一条举报记录。
	 *
	 * @param message 被举报消息的 UUID
	 * @param user 发起举报的用户 UUID
	 * @param timestamp 举报发生的时间戳
	 * @return 如果该用户此前没有举报过该消息，返回 true；否则返回 false
	 */
	boolean addReport(UUID message, UUID user, long timestamp) {
		Map<UUID, Report> reportsByUser = reportsByMessage.computeIfAbsent(message, key -> new HashMap<>());
		if (reportsByUser.containsKey(user)) return false;

		reportsByUser.put(user, new Report(message, user, timestamp));
		return true;
	}

	/**
	 * 删除一条举报记录。
	 *
	 * @param message 被取消举报的消息 UUID
	 * @param user 取消举报的用户 UUID
	 * @return 如果存在对应的活跃举报并成功删除，返回 true；否则返回 false
	 */
	boolean removeReport(UUID message, UUID user) {
		Map<UUID, Report> reportsByUser = reportsByMessage.get(message);
		if (reportsByUser == null || !reportsByUser.containsKey(user)) return false;

		reportsByUser.remove(user);
		if (reportsByUser.isEmpty()) reportsByMessage.remove(message);
		return true;
	}

	/**
	 * 判断指定用户是否正在举报指定消息。
	 *
	 * @param message 待检查消息的 UUID
	 * @param user 待检查用户的 UUID
	 * @return 如果存在对应的活跃举报，返回 true；否则返回 false
	 */
	boolean hasReported(UUID message, UUID user) {
		Map<UUID, Report> reportsByUser = reportsByMessage.get(message);
		return reportsByUser != null && reportsByUser.containsKey(user);
	}
}

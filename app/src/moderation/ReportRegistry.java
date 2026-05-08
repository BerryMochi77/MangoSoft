package moderation;

import dao.PostDAO;
import dao.model.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportRegistry {
	private static ReportRegistry instance;

	public static ReportRegistry getInstance() {
		if (instance == null) instance = new ReportRegistry();
		return instance;
	}

	private final Map<UUID, Map<UUID, Report>> reportsByMessage = new HashMap<>();

	boolean addReport(UUID message, UUID user, long timestamp) {
		Map<UUID, Report> reportsByUser =
				reportsByMessage.computeIfAbsent(message, key -> new HashMap<>());

		if (reportsByUser.containsKey(user)) {
			return false;
		}

		reportsByUser.put(user, new Report(message, user, timestamp));
		return true;
	}

	public void clear() {
		reportsByMessage.clear();
	}

	public Iterator<Report> getAllReports() {
		List<Report> reports = new ArrayList<>();
		for (Map<UUID, Report> reportsByUser : reportsByMessage.values()) {
			reports.addAll(reportsByUser.values());
		}
		return reports.iterator();
	}

	boolean removeReport(UUID message, UUID user) {
		Map<UUID, Report> reportsByUser = reportsByMessage.get(message);

		if (reportsByUser == null || !reportsByUser.containsKey(user)) {
			return false;
		}

		reportsByUser.remove(user);

		if (reportsByUser.isEmpty()) {
			reportsByMessage.remove(message);
		}

		return true;
	}

	boolean hasReported(UUID message, UUID user) {
		Map<UUID, Report> reportsByUser = reportsByMessage.get(message);
		return reportsByUser != null && reportsByUser.containsKey(user);
	}

	int getReportCount(UUID message) {
		Map<UUID, Report> reportsByUser = reportsByMessage.get(message);

		if (reportsByUser == null) {
			return 0;
		}

		return reportsByUser.size();
	}

	List<Message> getReportedMessagesByOldest() {
		List<Message> result = new ArrayList<>();

		for (UUID messageId : reportsByMessage.keySet()) {
			Message message = findMessage(messageId);

			if (message != null) {
				result.add(message);
			}
		}

		result.sort(Comparator.comparingLong(message ->
				getOldestReportTimestamp(message.id())
		));

		return result;
	}

	List<Message> getReportedMessagesByMost() {
		List<Message> result = new ArrayList<>();

		for (UUID messageId : reportsByMessage.keySet()) {
			Message message = findMessage(messageId);

			if (message != null) {
				result.add(message);
			}
		}

		result.sort((message1, message2) ->
				Integer.compare(
						getReportCount(message2.id()),
						getReportCount(message1.id())
				)
		);

		return result;
	}

	private long getOldestReportTimestamp(UUID messageId) {
		long oldest = Long.MAX_VALUE;
		Map<UUID, Report> reportsByUser = reportsByMessage.get(messageId);

		for (Report report : reportsByUser.values()) {
			if (report.timestamp() < oldest) {
				oldest = report.timestamp();
			}
		}

		return oldest;
	}

	private Message findMessage(UUID messageId) {
		Iterator<Message> messages = PostDAO.getInstance().getAllMessages();

		while (messages.hasNext()) {
			Message message = messages.next();

			if (message.id().equals(messageId)) {
				return message;
			}
		}

		return null;
	}
}

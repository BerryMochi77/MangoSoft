package persistentdata;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.PostHashtagEntry;
import dao.model.User;
import hashtag.HashtagService;
import moderation.ModerationTools;
import moderation.Report;
import moderation.ReportRegistry;
import persistentdata.formatted.CSVFormat;
import persistentdata.formatted.CSVFormattedFactory;
import persistentdata.io.ComputerIOFactory;
import persistentdata.io.IOFactory;
import persistentdata.serialization.HiddenMessageSerializer;
import persistentdata.serialization.MessageSerializer;
import persistentdata.serialization.PostHashtagSerializer;
import persistentdata.serialization.PostSerializer;
import persistentdata.serialization.ReportSerializer;
import persistentdata.serialization.UserSerializer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class DataManager {
	private static DataManager instance;
	public static DataManager getInstance() {
		if (instance == null)
			instance = new DataManager();
		return instance;
	}

	private final IOFactory IO = new ComputerIOFactory();

	// We have assumed that most solutions to the serialization task in week-5 will
	// use a 4-column schema for Users. If this is not the case, you may need to
	// change the number below.
	private final DataPipeline<User, String[]> userPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(4)), new UserSerializer(), "users");

	private final DataPipeline<Post, String[]> postPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(5)), new PostSerializer(), "posts");

	private final DataPipeline<Message, String[]> messagePipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(8)), new MessageSerializer(), "messages");

	private final DataPipeline<UUID, String[]> hiddenMessagePipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(1)), new HiddenMessageSerializer(), "hidden_messages");

	private final DataPipeline<Report, String[]> reportPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(4)), new ReportSerializer(), "reports");

	private final DataPipeline<PostHashtagEntry, String[]> postHashtagPipeline = new DataPipeline<>(
			IO, new CSVFormattedFactory(new CSVFormat(2)), new PostHashtagSerializer(), "post_hashtags");

	private final UserDAO users = UserDAO.getInstance();
	private final PostDAO posts = PostDAO.getInstance();
	private final ReportRegistry reports = ReportRegistry.getInstance();

	public void readAll() {
		users.clear();
		posts.clear();
		reports.clear();
		HashtagService.getInstance().clear();
		userPipeline.readTo(users::add);
		postPipeline.readTo(posts::add);
		messagePipeline.readTo(this::addMessageToPost);
		hiddenMessagePipeline.readTo(this::markMessageHidden);
		reportPipeline.readTo((report) ->
				ModerationTools.addReport(report.message(), report.user(), report.timestamp())
		);
		postHashtagPipeline.readTo(this::addHashtagToPost);
		HashtagService.getInstance().rebuildIndex();
	}

	public void writeAll() {
		userPipeline.writeFrom(users.getAll());
		postPipeline.writeFrom(posts.getAll());
		messagePipeline.writeFrom(posts.getAllMessages());
		hiddenMessagePipeline.writeFrom(getHiddenMessageIds());
		reportPipeline.writeFrom(reports.getAllReports());
		postHashtagPipeline.writeFrom(getAllHashtagEntries());
	}

	private void addMessageToPost(Message message) {
		Post post = posts.get(new Post(message.thread()));
		if (post != null) {
			post.messages.insert(message);
		}
	}

	private void markMessageHidden(UUID messageId) {
		Message message = findMessage(messageId);
		if (message != null) {
			message.setHidden(true);
		}
	}

	private Iterator<UUID> getHiddenMessageIds() {
		List<UUID> hiddenMessageIds = new ArrayList<>();
		Iterator<Message> messages = posts.getAllMessages();

		while (messages.hasNext()) {
			Message message = messages.next();
			if (message.isHidden()) {
				hiddenMessageIds.add(message.id());
			}
		}

		return hiddenMessageIds.iterator();
	}

	private void addHashtagToPost(PostHashtagEntry entry) {
		Post post = posts.get(new Post(entry.postId()));
		if (post != null) post.addHashtag(entry.tag());
	}

	private Iterator<PostHashtagEntry> getAllHashtagEntries() {
		List<PostHashtagEntry> entries = new ArrayList<>();
		Iterator<Post> it = posts.getAll();
		while (it.hasNext()) {
			Post post = it.next();
			for (String tag : post.getHashtags()) {
				entries.add(new PostHashtagEntry(post.id, tag));
			}
		}
		return entries.iterator();
	}

	private Message findMessage(UUID messageId) {
		Iterator<Message> messages = posts.getAllMessages();

		while (messages.hasNext()) {
			Message message = messages.next();
			if (message.id().equals(messageId)) {
				return message;
			}
		}

		return null;
	}
}

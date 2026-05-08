import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import moderation.ModerationTools;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class ModerationToolsAddReportTests {
	private Post post;
	private Message message;
	private User reporter;

	@Before
	public void setUp() {
		PostDAO.getInstance().clear();
		UserDAO.getInstance().clear();

		reporter = new User(UUID.randomUUID(), User.Role.Member, "reporter", "password");
		post = new Post(UUID.randomUUID(), reporter.getUUID(), "topic");
		message = new Message(UUID.randomUUID(), reporter.getUUID(), post.getUUID(), 1L, "hello");

		assertTrue(UserDAO.getInstance().add(reporter));
		assertTrue(PostDAO.getInstance().add(post));
		assertTrue(post.messages.insert(message));
	}

	@Test
	public void addReportSucceedsForExistingMessageAndUser() {
		assertTrue(ModerationTools.addReport(message.id(), reporter.getUUID(), 10L));
		assertTrue(ModerationTools.hasReported(message.id(), reporter.getUUID()));
	}

	@Test
	public void addReportRejectsDuplicateReportFromSameUser() {
		assertTrue(ModerationTools.addReport(message.id(), reporter.getUUID(), 10L));
		assertFalse(ModerationTools.addReport(message.id(), reporter.getUUID(), 20L));
		assertTrue(ModerationTools.hasReported(message.id(), reporter.getUUID()));
	}

	@Test
	public void addReportRejectsNullMessage() {
		assertFalse(ModerationTools.addReport(null, reporter.getUUID(), 10L));
	}

	@Test
	public void addReportRejectsMissingMessage() {
		assertFalse(ModerationTools.addReport(UUID.randomUUID(), reporter.getUUID(), 10L));
		assertFalse(ModerationTools.hasReported(message.id(), reporter.getUUID()));
	}

	@Test
	public void addReportRejectsNullUser() {
		assertFalse(ModerationTools.addReport(message.id(), null, 10L));
	}

	@Test
	public void addReportRejectsMissingUser() {
		UUID missingUser = UUID.randomUUID();
		assertFalse(ModerationTools.addReport(message.id(), missingUser, 10L));
		assertFalse(ModerationTools.hasReported(message.id(), missingUser));
	}
}

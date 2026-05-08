import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import moderation.ModerationTools;
import sorteddata.SortedData;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.UUID;

import static org.junit.Assert.*;

public class ModerationToolsSetHiddenTests {
	private Post post;
	private Message message;
	private User admin;
	private User member;

	@Before
	public void setUp() {
		PostDAO.getInstance().clear();
		UserDAO.getInstance().clear();

		admin = new User(UUID.randomUUID(), User.Role.Admin, "adminUser", "password");
		member = new User(UUID.randomUUID(), User.Role.Member, "memberUser", "password");
		post = new Post(UUID.randomUUID(), member.getUUID(), "topic");
		message = new Message(UUID.randomUUID(), member.getUUID(), post.getUUID(), 1L, "hello");

		assertTrue(UserDAO.getInstance().add(admin));
		assertTrue(UserDAO.getInstance().add(member));
		assertTrue(PostDAO.getInstance().add(post));
		assertTrue(post.messages.insert(message));
	}

	@Test
	public void adminCanHideMessage() {
		assertTrue(ModerationTools.setHidden(message.id(), admin.getUUID(), true));
		assertTrue(message.isHidden());
	}

	@Test
	public void adminCanUnhideMessage() {
		message.setHidden(true);

		assertTrue(ModerationTools.setHidden(message.id(), admin.getUUID(), false));
		assertFalse(message.isHidden());
	}

	@Test
	public void nonAdminCannotHideMessage() {
		assertFalse(ModerationTools.setHidden(message.id(), member.getUUID(), true));
		assertFalse(message.isHidden());
	}

	@Test
	public void invalidUserReturnsFalse() {
		assertFalse(ModerationTools.setHidden(message.id(), UUID.randomUUID(), true));
		assertFalse(message.isHidden());
	}

	@Test
	public void invalidMessageReturnsFalse() {
		assertFalse(ModerationTools.setHidden(UUID.randomUUID(), admin.getUUID(), true));
		assertFalse(message.isHidden());
	}

	@Test
	public void adminSeesHiddenMessages() {
		message.setHidden(true);

		SortedData<Message> visibleMessages = post.getVisibleMessages(true);

		assertTrue(contains(visibleMessages, message));
		assertEquals(1, count(visibleMessages));
	}

	@Test
	public void nonAdminAndGuestDoNotSeeHiddenMessages() {
		message.setHidden(true);

		SortedData<Message> visibleMessages = post.getVisibleMessages(false);

		assertFalse(contains(visibleMessages, message));
		assertEquals(0, count(visibleMessages));
	}

	@Test
	public void messagesAreVisibleByDefault() {
		assertFalse(message.isHidden());
		assertTrue(contains(post.getVisibleMessages(false), message));
	}

	private boolean contains(SortedData<Message> messages, Message target) {
		for (Iterator<Message> iterator = messages.getAll(); iterator.hasNext(); ) {
			if (iterator.next() == target) return true;
		}
		return false;
	}

	private int count(SortedData<Message> messages) {
		int count = 0;
		for (Iterator<Message> iterator = messages.getAll(); iterator.hasNext(); ) {
			iterator.next();
			count++;
		}
		return count;
	}
}

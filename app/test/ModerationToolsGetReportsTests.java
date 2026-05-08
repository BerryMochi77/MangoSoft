import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import moderation.ModerationTools;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class ModerationToolsGetReportsTests {
	private Post post;
	private Message m1, m2, m3;
	private User u1, u2, u3;

	@Before
	public void setUp() {
		PostDAO.getInstance().clear();
		UserDAO.getInstance().clear();

		u1 = new User(UUID.randomUUID(), User.Role.Member, "u1", "p");
		u2 = new User(UUID.randomUUID(), User.Role.Member, "u2", "p");
		u3 = new User(UUID.randomUUID(), User.Role.Member, "u3", "p");

		post = new Post(UUID.randomUUID(), u1.getUUID(), "topic");
		m1 = new Message(UUID.randomUUID(), u1.getUUID(), post.getUUID(), 1L, "m1");
		m2 = new Message(UUID.randomUUID(), u2.getUUID(), post.getUUID(), 2L, "m2");
		m3 = new Message(UUID.randomUUID(), u3.getUUID(), post.getUUID(), 3L, "m3");

		assertTrue(UserDAO.getInstance().add(u1));
		assertTrue(UserDAO.getInstance().add(u2));
		assertTrue(UserDAO.getInstance().add(u3));

		assertTrue(PostDAO.getInstance().add(post));
		assertTrue(post.messages.insert(m1));
		assertTrue(post.messages.insert(m2));
		assertTrue(post.messages.insert(m3));

		// Reports:
		// m1: one report at ts=10 (u1)
		// m2: two reports at ts=5 (u2) and ts=15 (u3)
		// m3: no reports
		assertTrue(ModerationTools.addReport(m1.id(), u1.getUUID(), 10L));
		assertTrue(ModerationTools.addReport(m2.id(), u2.getUUID(), 5L));
		assertTrue(ModerationTools.addReport(m2.id(), u3.getUUID(), 15L));
	}

	@Test
	public void mostStrategyReturnsByReportCountDescending() {
		Iterator<Message> it = ModerationTools.getReportedMessages("MOST", 2);
		assertNotNull(it);

		assertTrue(it.hasNext());
		Message first = it.next();
		assertTrue(it.hasNext());
		Message second = it.next();

		// m2 had 2 reports, m1 had 1
		assertEquals(m2, first);
		assertEquals(m1, second);
		assertFalse(it.hasNext());
	}

	@Test
	public void oldestStrategyReturnsByOldestReportTimestamp() {
		Iterator<Message> it = ModerationTools.getReportedMessages("OLDEST", 2);
		assertNotNull(it);

		assertTrue(it.hasNext());
		Message first = it.next();
		assertTrue(it.hasNext());
		Message second = it.next();

		// m2's oldest report is at ts=5, m1's oldest is at ts=10
		assertEquals(m2, first);
		assertEquals(m1, second);
		assertFalse(it.hasNext());
	}

	@Test
	public void doesNotReturnMessagesWithZeroReports() {
		Iterator<Message> it = ModerationTools.getReportedMessages("MOST", 10);
		Set<Message> results = new HashSet<>();
		while (it.hasNext()) results.add(it.next());

		assertFalse(results.contains(m3));
	}

	@Test
	public void amountLargerThanAvailableReturnsFewer() {
		Iterator<Message> it = ModerationTools.getReportedMessages("MOST", 10);
		int count = 0;
		while (it.hasNext()) { it.next(); count++; }
		// only m1 and m2 have reports
		assertEquals(2, count);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidStrategyThrows() {
		ModerationTools.getReportedMessages("INVALID", 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nonPositiveAmountThrows() {
		ModerationTools.getReportedMessages("OLDEST", 0);
	}

	@Test
	public void eachMessageReturnedAtMostOnce() {
		Iterator<Message> it = ModerationTools.getReportedMessages("MOST", 10);
		Set<Message> seen = new HashSet<>();
		while (it.hasNext()) {
			Message m = it.next();
			assertFalse("Message returned more than once", seen.contains(m));
			seen.add(m);
		}
	}
}

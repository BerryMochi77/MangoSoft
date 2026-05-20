package dao;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;
import messagestate.MessageThreadRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RandomContentGenerator {
	private static final String[] POST_TITLES = {
			"How should we structure the moderation module? #moderation #help",
			"Question about Android Studio layouts #help #android",
			"Study session for data structures #study",
			"How to implement modelling? #help",
			"This post looks like spam to me #spam #moderation",
			"Design patterns in the mini project #design",
			"Debugging RecyclerView adapters #android #bug",
			"Harassment report thread #harassment #abuse #moderation"
	};

	private static final String[] REPLIES = {
			"I think keeping UI and core logic separate makes this much easier to maintain.",
			"Using a smaller data set helps a lot when checking the interface.",
			"The report flow is clearer now that admins can review details.",
			"This would be a good place to add tests before adding more features.",
			"Maybe this belongs in social-core rather than the Android activity.",
			"I agree. The model should stay reusable for future features.",
			"Could we add a follow-up screen for user profiles later?",
			"This is readable on mobile now."
	};

	/**
	 * Conversational follow-ups used when seeding multi-level nested replies
	 * for the demo. Kept short so deep nesting is still readable on a phone.
	 */
	private static final String[] FOLLOW_UPS = {
			"Good point — that makes the registry approach cleaner.",
			"Right, but doesn't that couple Message back to the UI?",
			"It does if we let it. Hence the sidecar.",
			"Yeah I see it now, +1.",
			"Strong agree. Edit history fits the same template.",
			"What about deletion though? Same registry or separate?",
			"Separate. Single responsibility, easier to test.",
			"Fair. SOLID wins.",
			"How does this scale if we ever add threading like Reddit?",
			"You just add another registry. Message stays untouched.",
			"Beautiful. Demo-able.",
			"😂 sold."
	};

	private static final int POST_COUNT = 8;
	private static final int MIN_REPLIES_PER_POST = 3;
	private static final int EXTRA_REPLIES_PER_POST = 3;
	private static final Random random = new Random(2100);

	public static void populateRandomData() {
		List<User> users = getExistingUsers();
		if (users.isEmpty()) return;

		for (int i = 0; i < POST_COUNT; i++) {
			User poster = users.get(i % users.size());
			String title = POST_TITLES[i % POST_TITLES.length];
			Post post = new Post(UUID.randomUUID(), poster.getUUID(), title);
			post.setHashtags(HashtagParser.extract(title));
			PostDAO.getInstance().add(post);
			HashtagService.getInstance().indexPost(post);
			populateReplies(post, users, i);
		}
	}

	private static void populateReplies(Post post, List<User> users, int postIndex) {
		int replyCount = MIN_REPLIES_PER_POST + random.nextInt(EXTRA_REPLIES_PER_POST + 1);
		List<Message> topLevel = new ArrayList<>();
		for (int i = 0; i < replyCount; i++) {
			User user = users.get((postIndex + i + 1) % users.size());
			String content = REPLIES[(postIndex + i) % REPLIES.length];
			long timestamp = System.currentTimeMillis() - ((long) (postIndex * 10 + i) * 60_000L);
			Message message = new Message(UUID.randomUUID(), user.getUUID(), post.getUUID(), timestamp, content);
			post.messages.insert(message);
			topLevel.add(message);
		}

		seedNestedReplies(post, users, postIndex, topLevel);
	}

	/**
	 * Hang follow-up replies off some of the top-level comments so the demo
	 * actually exercises {@link MessageThreadRegistry} and shows the
	 * Reddit-style indent. Depth distribution per post:
	 *
	 * <ul>
	 *   <li>The first top-level reply gets a small subtree, 2–3 levels deep.</li>
	 *   <li>Another top-level reply gets one direct child to show
	 *       single-level nesting.</li>
	 * </ul>
	 */
	private static void seedNestedReplies(Post post, List<User> users, int postIndex,
	                                      List<Message> topLevel) {
		if (topLevel.isEmpty()) return;

		MessageThreadRegistry threads = MessageThreadRegistry.getInstance();
		long baseTime = System.currentTimeMillis() - ((long) postIndex * 600_000L);
		int[] followCursor = {postIndex * 3}; // cycle through FOLLOW_UPS without repeating

		// Subtree under topLevel[0]: depth 1 -> depth 2 -> depth 3.
		Message child1 = addReplyUnder(post, users, postIndex, topLevel.get(0),
				baseTime + 60_000L, followCursor);
		threads.setParent(child1.id(), topLevel.get(0).id());

		Message grandchild = addReplyUnder(post, users, postIndex, topLevel.get(0),
				baseTime + 120_000L, followCursor);
		threads.setParent(grandchild.id(), child1.id());

		Message greatGrandchild = addReplyUnder(post, users, postIndex, topLevel.get(0),
				baseTime + 180_000L, followCursor);
		threads.setParent(greatGrandchild.id(), grandchild.id());

		// Sibling reply at depth 1 to show fan-out, not just a single chain.
		Message sibling = addReplyUnder(post, users, postIndex, topLevel.get(0),
				baseTime + 240_000L, followCursor);
		threads.setParent(sibling.id(), topLevel.get(0).id());

		// One more single-level nest under a different top-level reply.
		if (topLevel.size() >= 2) {
			Message other = addReplyUnder(post, users, postIndex, topLevel.get(1),
					baseTime + 300_000L, followCursor);
			threads.setParent(other.id(), topLevel.get(1).id());
		}
	}

	private static Message addReplyUnder(Post post, List<User> users, int postIndex,
	                                     Message parent, long timestamp, int[] followCursor) {
		// Pick an author distinct from the parent's author when possible so
		// the conversation actually looks like a back-and-forth.
		User author = users.get((postIndex + followCursor[0] + 2) % users.size());
		if (author.getUUID().equals(parent.poster()) && users.size() > 1) {
			author = users.get((postIndex + followCursor[0] + 3) % users.size());
		}
		String content = FOLLOW_UPS[Math.floorMod(followCursor[0], FOLLOW_UPS.length)];
		followCursor[0]++;
		Message reply = new Message(UUID.randomUUID(), author.getUUID(), post.getUUID(), timestamp, content);
		post.messages.insert(reply);
		return reply;
	}

	private static List<User> getExistingUsers() {
		ArrayList<User> users = new ArrayList<>();
		for (var iterator = UserDAO.getInstance().getAll(); iterator.hasNext(); ) {
			User user = iterator.next();
			if (user.username() != null && user.role() == User.Role.Member) {
				users.add(user);
			}
		}
		return users;
	}
}

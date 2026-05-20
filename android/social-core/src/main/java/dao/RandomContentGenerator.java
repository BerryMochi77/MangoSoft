package dao;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;
import messagestate.MessageThreadRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

	private static final String[] POST_BODIES = {
			"Here is the screen state I was looking at.",
			"Small visual examples make the discussion easier to follow.",
			"This would be clearer with a screenshot attached.",
			"I added a quick mockup so the idea is less abstract."
	};

	private static final String[] EMOJIS = {
			"\uD83D\uDE42",
			"\uD83D\uDE02",
			"\uD83D\uDC4D",
			"\uD83D\uDD25",
			"\uD83C\uDF89"
	};

	private static final String[] DEMO_IMAGE_TOKENS = {
			"[[image:demo:coastal]]",
			"[[image:demo:forest]]",
			"[[image:demo:violet]]"
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
	private static final Set<String> DEMO_USERNAMES = new HashSet<>(Arrays.asList(
			"alex",
			"beatrice",
			"carmen",
			"diego",
			"emma",
			"farah",
			"georg",
			"hadrian",
			"iris",
			"june"
	));

	public static void populateRandomData() {
		List<User> users = getExistingUsers();
		if (users.isEmpty()) return;

		for (int i = 0; i < POST_COUNT; i++) {
			User poster = users.get(i % users.size());
			String title = POST_TITLES[i % POST_TITLES.length];
			Post post = new Post(UUID.randomUUID(), poster.getUUID(), title);
			String body = demoPostBody(i);
			post.setBody(body);
			post.setHashtags(HashtagParser.extract(title + " " + body));
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
			String content = demoReplyContent(REPLIES[(postIndex + i) % REPLIES.length], postIndex, i);
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
		String content = demoReplyContent(
				FOLLOW_UPS[Math.floorMod(followCursor[0], FOLLOW_UPS.length)],
				postIndex,
				followCursor[0]
		);
		followCursor[0]++;
		Message reply = new Message(UUID.randomUUID(), author.getUUID(), post.getUUID(), timestamp, content);
		post.messages.insert(reply);
		return reply;
	}

	private static String demoPostBody(int index) {
		if (index % 3 == 1) {
			return POST_BODIES[index % POST_BODIES.length]
					+ " "
					+ EMOJIS[index % EMOJIS.length]
					+ "\n"
					+ DEMO_IMAGE_TOKENS[index % DEMO_IMAGE_TOKENS.length];
		}
		if (index % 3 == 2) {
			return POST_BODIES[index % POST_BODIES.length] + " " + EMOJIS[index % EMOJIS.length];
		}
		return "";
	}

	private static String demoReplyContent(String base, int postIndex, int replyIndex) {
		String content = base;
		if ((postIndex + replyIndex) % 2 == 0) {
			content += " " + EMOJIS[Math.floorMod(postIndex + replyIndex, EMOJIS.length)];
		}
		if ((postIndex + replyIndex) % 5 == 0) {
			content += "\n" + DEMO_IMAGE_TOKENS[Math.floorMod(postIndex + replyIndex, DEMO_IMAGE_TOKENS.length)];
		}
		return content;
	}

	private static List<User> getExistingUsers() {
		ArrayList<User> users = new ArrayList<>();
		for (var iterator = UserDAO.getInstance().getAll(); iterator.hasNext(); ) {
			User user = iterator.next();
			if (user.username() != null
					&& user.role() == User.Role.Member
					&& DEMO_USERNAMES.contains(user.username().toLowerCase())) {
				users.add(user);
			}
		}
		return users;
	}
}

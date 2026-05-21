package dao;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;
import messagestate.MessageDeletionRegistry;
import messagestate.MessageThreadRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RandomContentGenerator {
	private static final String[] LEGACY_POST_TITLES = {
			"How should we structure the moderation module? #moderation #help",
			"Question about Android Studio layouts #help #android",
			"Study session for data structures #study",
			"How to implement modelling? #help",
			"This post looks like spam to me #spam #moderation",
			"Design patterns in the mini project #design",
			"Debugging RecyclerView adapters #android #bug",
			"Harassment report thread #harassment #abuse #moderation",
			"Best way to test DAO classes? #testing #dao",
			"Avatar crop edge cases #android #profile",
			"How should notifications work? #notification #design",
			"Data persistence checklist #persistentdata #testing",
			"Nested replies and moderation UX #threading #moderation",
			"Good examples for report evidence #reporting #help"
	};

	/**
	 * University-forum style demo posts (think Ed Discussion): course
	 * announcements, assignments, exams, careers, academic discussion, and
	 * student-life topics. Appended after {@link #LEGACY_POST_TITLES} so the
	 * existing seeded data and repair logic stay valid (indices only grow).
	 * Each entry is {@code {title-with-trailing-#tags, body}}; bodies feed
	 * hashtag extraction and the AI feed filter, so they are intentionally
	 * varied in topic.
	 */
	private static final String[][] EXTRA_POSTS = {
			{"Assignment 2 is now released - due Week 8 #assignment #comp2100 #deadline",
					"The Assignment 2 specification is up on the course page. You'll implement a hash table and analyse its complexity. Submissions close 11:59pm Friday of Week 8. Start early and use the forum for questions. 📌"},
			{"Week 6 lecture moved to a different theatre #announcement #lectures",
					"Heads up: this week's lecture is relocated due to a room clash. The recording will still be posted afterwards."},
			{"Final exam format and revision sessions #exam #revision #study",
					"The final is 3 hours, closed book, covering Weeks 1-11. We'll run two revision sessions in Week 12 - bring your questions. Past papers are linked under Resources."},
			{"Tech Careers Fair next Thursday on campus #careers #jobs #event",
					"Over 30 employers will be on campus from 10am to 3pm. Bring printed CVs. Several software companies and the public service are confirmed - a great chance for internship leads. 🎉"},
			{"Summer internship applications now open #internship #careers #softwareengineering",
					"Several summer software engineering internships opened this week, and most close at the end of the month. Happy to review CVs if you want feedback."},
			{"Why is quicksort n log n on average but quadratic worst case? #algorithms #study #discussion",
					"Trying to build intuition for the pivot choice. Is randomised pivoting enough in practice, or do we need median-of-medians? Keen to hear how people reason about this."},
			{"Forming a study group for data structures #study #studygroup #datastructures",
					"Looking for 3-4 people to meet weekly in the library. We'll work through tutorial problems and past exams. Comment if you're interested."},
			{"Undergraduate research opportunity in HCI #research #hci #opportunity",
					"Our lab is taking on two undergrad research assistants for a human-computer interaction project next semester. Some Android experience is a plus. Email if interested."},
			{"Hackathon this weekend - teams of up to four #hackathon #event #club",
					"The Computing Students' Association is running a 24-hour hackathon on Saturday. Free food, mentors, and prizes. Register by Friday. 🔥\n[[image:demo:forest]]"},
			{"Tutorial allocations are now live #announcement #tutorials",
					"Check your tutorial allocation on the timetable system. If you have an unavoidable clash, submit a change request by Wednesday."},
			{"Reminder: academic support and wellbeing services #wellbeing #support #studentlife",
					"If the semester is getting heavy, the university offers free counselling and academic skills workshops. There's no shame in reaching out early - take care of yourselves. 🙂"},
			{"Group project teams due by end of Week 5 #groupproject #deadline #softwareengineering",
					"Please finalise your group project teams of three and register them on the course page by Friday of Week 5. Unallocated students will be paired automatically."}
	};

	/** All demo post titles: the legacy set first, then the university-forum posts. */
	private static final String[] POST_TITLES = buildPostTitles();

	private static String[] buildPostTitles() {
		String[] titles = new String[LEGACY_POST_TITLES.length + EXTRA_POSTS.length];
		System.arraycopy(LEGACY_POST_TITLES, 0, titles, 0, LEGACY_POST_TITLES.length);
		for (int i = 0; i < EXTRA_POSTS.length; i++) {
			titles[LEGACY_POST_TITLES.length + i] = EXTRA_POSTS[i][0];
		}
		return titles;
	}

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

	/** Neutral, topic-agnostic top-level replies for the university-forum posts. */
	private static final String[] EXTRA_REPLIES = {
			"Thanks for the heads up!",
			"This is really helpful, appreciated.",
			"Will this be recorded for anyone who can't attend?",
			"Just registered - looking forward to it.",
			"Is this open to first-years as well?",
			"Great initiative, count me in.",
			"Could you share the slides afterwards?",
			"Following - same question here."
	};

	/** Neutral nested follow-ups for the university-forum posts. */
	private static final String[] EXTRA_FOLLOW_UPS = {
			"Good question, I was wondering the same.",
			"I think it was mentioned in the announcement.",
			"Confirmed - it's on the course page now.",
			"Thanks, that clears it up!",
			"See you there 🎉",
			"+1, would love a recording.",
			"Makes sense, appreciate the reply.",
			"Same here, just signed up."
	};
	private static final Set<String> GENERATED_REPLY_TEXTS = new HashSet<>();

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

	private static final int POST_COUNT = POST_TITLES.length;
	private static final int MIN_REPLIES_PER_POST = 3;
	private static final int EXTRA_REPLIES_PER_POST = 3;
	private static final long DEMO_POST_BASE_TIME = 1_768_780_800_000L;
	private static final long DEMO_POST_INTERVAL_MS = 43_200_000L;
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

	static {
		GENERATED_REPLY_TEXTS.addAll(Arrays.asList(REPLIES));
		GENERATED_REPLY_TEXTS.addAll(Arrays.asList(FOLLOW_UPS));
		GENERATED_REPLY_TEXTS.addAll(Arrays.asList(EXTRA_REPLIES));
		GENERATED_REPLY_TEXTS.addAll(Arrays.asList(EXTRA_FOLLOW_UPS));
		for (String base : REPLIES) {
			for (int postIndex = 0; postIndex < POST_COUNT; postIndex++) {
				for (int replyIndex = 0; replyIndex < 16; replyIndex++) {
					GENERATED_REPLY_TEXTS.add(demoReplyContent(base, postIndex, replyIndex));
				}
			}
		}
		for (String base : FOLLOW_UPS) {
			for (int postIndex = 0; postIndex < POST_COUNT; postIndex++) {
				for (int replyIndex = 0; replyIndex < 48; replyIndex++) {
					GENERATED_REPLY_TEXTS.add(demoReplyContent(base, postIndex, replyIndex));
				}
			}
		}
	}

	public static void populateRandomData() {
		List<User> users = getExistingUsers();
		if (users.isEmpty()) return;

		for (int i = 0; i < POST_COUNT; i++) {
			createDemoPost(users, i);
		}
	}

	public static void repairSeededData() {
		HashtagService hashtags = HashtagService.getInstance();
		Set<String> existingDemoTopics = new HashSet<>();
		Iterator<Post> posts = PostDAO.getInstance().getAll();
		while (posts.hasNext()) {
			Post post = posts.next();
			int demoIndex = demoPostIndex(post.topic);
			if (demoIndex >= 0) {
				existingDemoTopics.add(post.topic);
				post.setCreatedAt(demoPostTime(demoIndex));
				String body = demoPostBody(demoIndex);
				if (post.getBody().isEmpty() && !body.isEmpty()) {
					post.setBody(body);
					post.setHashtags(HashtagParser.extract(post.topic + " " + body));
					hashtags.indexPost(post);
				}
			}
			purgeMisassignedGeneratedReplies(post);
		}
		ensureMissingDemoPosts(existingDemoTopics);
	}

	private static Post createDemoPost(List<User> users, int postIndex) {
		User poster = users.get(postIndex % users.size());
		String title = POST_TITLES[postIndex % POST_TITLES.length];
		Post post = new Post(UUID.randomUUID(), poster.getUUID(), title);
		post.setCreatedAt(demoPostTime(postIndex));
		String body = demoPostBody(postIndex);
		post.setBody(body);
		post.setHashtags(HashtagParser.extract(title + " " + body));
		PostDAO.getInstance().add(post);
		HashtagService.getInstance().indexPost(post);
		populateReplies(post, users, postIndex);
		return post;
	}

	private static void ensureMissingDemoPosts(Set<String> existingDemoTopics) {
		List<User> users = getExistingUsers();
		if (users.isEmpty()) return;
		for (int i = 0; i < POST_COUNT; i++) {
			if (!existingDemoTopics.contains(POST_TITLES[i])) {
				createDemoPost(users, i);
			}
		}
	}

	private static void populateReplies(Post post, List<User> users, int postIndex) {
		int replyCount = MIN_REPLIES_PER_POST + random.nextInt(EXTRA_REPLIES_PER_POST + 1);
		boolean extra = isExtraPost(postIndex);
		List<Message> topLevel = new ArrayList<>();
		for (int i = 0; i < replyCount; i++) {
			User user = users.get((postIndex + i + 1) % users.size());
			String content = extra
					? EXTRA_REPLIES[(postIndex + i) % EXTRA_REPLIES.length]
					: demoReplyContent(REPLIES[(postIndex + i) % REPLIES.length], postIndex, i);
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
		String content = isExtraPost(postIndex)
				? EXTRA_FOLLOW_UPS[Math.floorMod(followCursor[0], EXTRA_FOLLOW_UPS.length)]
				: demoReplyContent(
						FOLLOW_UPS[Math.floorMod(followCursor[0], FOLLOW_UPS.length)],
						postIndex,
						followCursor[0]
				);
		followCursor[0]++;
		Message reply = new Message(UUID.randomUUID(), author.getUUID(), post.getUUID(), timestamp, content);
		post.messages.insert(reply);
		return reply;
	}

	/** True for the appended university-forum posts (indices past the legacy set). */
	private static boolean isExtraPost(int index) {
		return index >= LEGACY_POST_TITLES.length;
	}

	private static String demoPostBody(int index) {
		if (isExtraPost(index)) {
			return EXTRA_POSTS[index - LEGACY_POST_TITLES.length][1];
		}
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

	private static void purgeMisassignedGeneratedReplies(Post post) {
		MessageDeletionRegistry deletions = MessageDeletionRegistry.getInstance();
		for (var iterator = post.messages.getAll(); iterator.hasNext(); ) {
			Message message = iterator.next();
			User author = UserDAO.getInstance().getByUUID(message.poster());
			if (author == null || isDemoUsername(author.username())) continue;
			if (isGeneratedReplyContent(message.message())) {
				deletions.markDeleted(message.id());
			}
		}
	}

	private static boolean isGeneratedReplyContent(String content) {
		if (content == null) return false;
		String clean = content.trim();
		if (GENERATED_REPLY_TEXTS.contains(clean)) return true;
		for (String base : REPLIES) {
			if (clean.startsWith(base + " ")) return true;
			if (clean.startsWith(base + "\n")) return true;
		}
		for (String base : FOLLOW_UPS) {
			if (clean.startsWith(base + " ")) return true;
			if (clean.startsWith(base + "\n")) return true;
		}
		return false;
	}

	private static int demoPostIndex(String topic) {
		if (topic == null) return -1;
		for (int i = 0; i < POST_TITLES.length; i++) {
			if (POST_TITLES[i].equals(topic)) return i;
		}
		return -1;
	}

	private static long demoPostTime(int postIndex) {
		if (isExtraPost(postIndex)) {
			// Make the university-forum posts the most recent so they lead the
			// time-sorted feed and read as the "current" campus activity.
			int ordinal = postIndex - LEGACY_POST_TITLES.length;
			return DEMO_POST_BASE_TIME + (long) (ordinal + 1) * DEMO_POST_INTERVAL_MS;
		}
		return DEMO_POST_BASE_TIME - postIndex * DEMO_POST_INTERVAL_MS;
	}

	private static boolean isDemoUsername(String username) {
		return username != null && DEMO_USERNAMES.contains(username.toLowerCase());
	}

	private static List<User> getExistingUsers() {
		ArrayList<User> users = new ArrayList<>();
		for (var iterator = UserDAO.getInstance().getAll(); iterator.hasNext(); ) {
			User user = iterator.next();
			if (user.username() != null
					&& user.role() == User.Role.Member
					&& isDemoUsername(user.username())) {
				users.add(user);
			}
		}
		return users;
	}
}

package dao;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import hashtag.HashtagParser;
import hashtag.HashtagService;

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
		for (int i = 0; i < replyCount; i++) {
			User user = users.get((postIndex + i + 1) % users.size());
			String content = REPLIES[(postIndex + i) % REPLIES.length];
			long timestamp = System.currentTimeMillis() - ((long) (postIndex * 10 + i) * 60_000L);
			Message message = new Message(UUID.randomUUID(), user.getUUID(), post.getUUID(), timestamp, content);
			post.messages.insert(message);
		}
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

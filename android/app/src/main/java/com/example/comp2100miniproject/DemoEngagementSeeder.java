package com.example.comp2100miniproject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Post;
import dao.model.User;
import postview.PostViewService;

public final class DemoEngagementSeeder {
    private static final String[] DEMO_USERNAMES = {
            "alex", "beatrice", "carmen", "diego", "emma",
            "farah", "georg", "hadrian", "iris", "june"
    };
    private static final String[] EMOJIS = {
            "\uD83D\uDC4D",
            "\u2764\uFE0F",
            "\uD83D\uDE02",
            "\uD83D\uDE21",
            "\uD83D\uDE00",
            "\uD83D\uDD25"
    };

    private static boolean seededThisProcess = false;

    private DemoEngagementSeeder() {}

    public static void seedIfNeeded() {
        if (seededThisProcess) return;
        List<User> demoUsers = demoUsers();
        if (demoUsers.isEmpty()) return;

        ReactionManager reactions = ReactionManager.getInstance();
        PostViewService views = PostViewService.getInstance();

        int postIndex = 0;
        for (Iterator<Post> it = PostDAO.getInstance().getAll(); it.hasNext(); ) {
            Post post = it.next();
            if (post.isDeleted()) continue;

            int targetViews = 18 + (postIndex % 6) * 7 + (postIndex / 2) * 3;
            while (views.getViewCount(post.id) < targetViews) {
                views.recordView(post.id);
            }

            int reactionUsers = Math.min(demoUsers.size(), 3 + (postIndex % 5));
            for (int i = 0; i < reactionUsers; i++) {
                User user = demoUsers.get((postIndex + i) % demoUsers.size());
                String emoji = EMOJIS[(postIndex + i) % EMOJIS.length];
                reactions.addUserReaction(post.id, user.getUUID(), emoji);
                if ((postIndex + i) % 3 == 0) {
                    reactions.addUserReaction(post.id, user.getUUID(), EMOJIS[(postIndex + 2) % EMOJIS.length]);
                }
            }
            postIndex++;
        }

        seededThisProcess = true;
    }

    private static List<User> demoUsers() {
        ArrayList<User> users = new ArrayList<>();
        for (String username : DEMO_USERNAMES) {
            User user = UserDAO.getInstance().login(username, "123456");
            if (user != null) users.add(user);
        }
        return users;
    }
}

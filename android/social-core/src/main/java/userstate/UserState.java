package userstate;

import dao.model.Post;

public abstract class UserState {
	public abstract boolean isLoggedIn();

	public abstract UserState register(String username, String password);

	public abstract UserState logout();

	public abstract boolean addReply(Post post, String content);

    public abstract UserState login(String username, String password);
}

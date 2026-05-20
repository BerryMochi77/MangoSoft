package userstate;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;

import java.util.UUID;

public class MemberState extends UserState {
	public final User user;
	public MemberState(User user) {
		this.user = user;
	}

	@Override
	public boolean isLoggedIn() {
		return true;
	}


	@Override
	public UserState register(String username, String password) {
		return this;
	}

	@Override
	public UserState logout() {
		return new GuestState();
	}

	@Override
	public boolean addReply(Post post, String content) {
		post.messages.insert(new Message(UUID.randomUUID(), user.getUUID(), post.getUUID(), System.currentTimeMillis(), content));
		return true;
	}

    @Override
    public UserState login(String username, String password) {
        return this;
    }
}

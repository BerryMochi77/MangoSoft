package userstate;

import dao.model.Post;

public class StateManager {
	private static UserState state = new GuestState();

	public static UserState getState() {
		return state;
	}

	public static boolean login(String username, String password) {
		// TODO: Complete this method in accordance with the State design pattern
		// For this task, you may modify the other classes and interfaces within
		//  the userstate package by adding methods, including public ones
        UserState newState = state.login(username, password);
        boolean success = newState != state;
        state = newState;
        return success;
	}

	public static boolean register(String username, String password) {
		UserState newState = state.register(username, password);
		boolean success = newState != state;
		state = newState;
		return success;
	}

	public static boolean logout() {
		UserState newState = state.logout();
		boolean success = newState != state;
		state = newState;
		return success;
	}

	public static boolean post(Post post, String content) {
		return state.addReply(post, content);
	}

	public static boolean isLoggedIn() {
		return state.isLoggedIn();
	}
}

package com.example.comp2100miniproject.auth;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import dao.UserDAO;
import dao.model.User;

public class AuthManager {
    public static final String EXTRA_USER_ID = "current_user_id";
    public static final String EXTRA_IS_ADMIN = "current_user_is_admin";

    private static final String USERS_FILE = "users.json";
    private static final String ADMIN_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String[] DEMO_USERNAMES = {
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
    };

    private final Context context;
    private final UserDAO userDAO = UserDAO.getInstance();

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
        ensureDefaultUsers();
        loadUsersIntoDAO();
    }

    public User login(String username, String password) {
        User user = findStoredUser(username);
        if (user == null || !user.password().equals(password)) return null;
        userDAO.add(user);
        return user;
    }

    /**
     * Returns the stored user with the given username, or {@code null} if no
     * such user exists. Used by the login screen to look up an account's
     * avatar before the user finishes typing their password.
     */
    public User findUserByUsername(String username) {
        return findStoredUser(username);
    }

    public User register(String username, String password) {
        String cleanUsername = username == null ? "" : username.trim();
        if (!isValidUsername(cleanUsername) || password == null || password.length() < 4) return null;
        if (findStoredUser(cleanUsername) != null) return null;

        User user = new User(UUID.randomUUID(), User.Role.Member, cleanUsername, password);
        JSONArray users = readUsers();
        users.put(toJson(user, cleanUsername));
        writeUsers(users);
        userDAO.add(user);
        return user;
    }

    public String getDisplayName(User user) {
        if (user == null) return "Unknown user";
        JSONObject json = findStoredUserJson(user.getUUID());
        if (json == null) return user.username();
        String displayName = json.optString("displayName", user.username()).trim();
        return displayName.isEmpty() ? user.username() : displayName;
    }

    public Avatar getAvatar(User user) {
        if (user == null) return Avatar.defaultAvatar();
        JSONObject json = findStoredUserJson(user.getUUID());
        if (json == null) return Avatar.defaultAvatar();

        String source = json.optString("avatarSource", Avatar.SOURCE_DEFAULT);
        String value = json.optString("avatarValue", Avatar.DEFAULT_VALUE);
        if (source.trim().isEmpty() || value.trim().isEmpty()) return Avatar.defaultAvatar();
        return new Avatar(source, value);
    }

    public ProfileBackground getProfileBackground(User user) {
        if (user == null) return ProfileBackground.defaultBackground();
        JSONObject json = findStoredUserJson(user.getUUID());
        if (json == null) return ProfileBackground.defaultBackground();

        String source = json.optString("profileBackgroundSource", ProfileBackground.SOURCE_DEFAULT);
        String value = json.optString("profileBackgroundValue", ProfileBackground.DEFAULT_VALUE);
        if (source.trim().isEmpty() || value.trim().isEmpty()) return ProfileBackground.defaultBackground();
        return new ProfileBackground(source, value);
    }

    public boolean updateAvatar(UUID userId, String source, String value) {
        if (userId == null || source == null || value == null || source.trim().isEmpty() || value.trim().isEmpty()) {
            return false;
        }

        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            JSONObject json = users.optJSONObject(i);
            if (json == null || !userId.toString().equals(json.optString("id"))) continue;

            try {
                json.put("avatarSource", source);
                json.put("avatarValue", value);
                writeUsers(users);
                return true;
            } catch (JSONException ignored) {
                return false;
            }
        }
        return false;
    }

    public boolean updateProfileBackground(UUID userId, String source, String value) {
        if (userId == null || source == null || value == null || source.trim().isEmpty() || value.trim().isEmpty()) {
            return false;
        }

        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            JSONObject json = users.optJSONObject(i);
            if (json == null || !userId.toString().equals(json.optString("id"))) continue;

            try {
                json.put("profileBackgroundSource", source);
                json.put("profileBackgroundValue", value);
                writeUsers(users);
                return true;
            } catch (JSONException ignored) {
                return false;
            }
        }
        return false;
    }

    public boolean updateProfile(UUID userId, String displayName, String newPassword) {
        if (userId == null) return false;

        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            JSONObject json = users.optJSONObject(i);
            if (json == null || !userId.toString().equals(json.optString("id"))) continue;

            try {
                String cleanDisplayName = displayName == null ? "" : displayName.trim();
                if (cleanDisplayName.isEmpty()) return false;
                json.put("displayName", cleanDisplayName);

                String cleanPassword = newPassword == null ? "" : newPassword;
                if (!cleanPassword.isEmpty()) {
                    if (cleanPassword.length() < 4) return false;
                    json.put("password", cleanPassword);
                }

                writeUsers(users);
                userDAO.clear();
                loadUsersIntoDAO();
                return true;
            } catch (JSONException ignored) {
                return false;
            }
        }
        return false;
    }

    public User getUser(UUID userId) {
        if (userId == null) return null;
        User user = userDAO.getByUUID(userId);
        if (user != null) return user;

        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            User stored = fromJson(users.optJSONObject(i));
            if (stored != null && userId.equals(stored.getUUID())) {
                userDAO.add(stored);
                return stored;
            }
        }
        return null;
    }

    private void ensureDefaultUsers() {
        JSONArray users = readUsers();
        boolean changed = false;

        if (findStoredUser(ADMIN_USERNAME) == null) {
            users.put(toJson(new User(UUID.randomUUID(), User.Role.Admin, ADMIN_USERNAME, DEFAULT_PASSWORD), "Admin"));
            changed = true;
        }

        for (String username : DEMO_USERNAMES) {
            if (findStoredUser(username) == null) {
                users.put(toJson(new User(UUID.randomUUID(), User.Role.Member, username, DEFAULT_PASSWORD), toDisplayName(username)));
                changed = true;
            }
        }

        if (changed) writeUsers(users);
    }

    private void loadUsersIntoDAO() {
        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            User user = fromJson(users.optJSONObject(i));
            if (user != null) userDAO.add(user);
        }
    }

    private User findStoredUser(String username) {
        if (username == null) return null;

        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            User user = fromJson(users.optJSONObject(i));
            if (user != null && user.username().equalsIgnoreCase(username.trim())) return user;
        }
        return null;
    }

    private boolean isValidUsername(String username) {
        if (username.length() < 4 || username.length() > 20) return false;
        for (char c : username.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) return false;
        }
        return true;
    }

    private JSONObject findStoredUserJson(UUID userId) {
        if (userId == null) return null;

        JSONArray users = readUsers();
        for (int i = 0; i < users.length(); i++) {
            JSONObject json = users.optJSONObject(i);
            if (json != null && userId.toString().equals(json.optString("id"))) return json;
        }
        return null;
    }

    private JSONArray readUsers() {
        File file = new File(context.getFilesDir(), USERS_FILE);
        if (!file.exists()) return new JSONArray();

        try (FileInputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = input.read(bytes);
            if (read <= 0) return new JSONArray();
            return new JSONArray(new String(bytes, 0, read, StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) {
            return new JSONArray();
        }
    }

    private void writeUsers(JSONArray users) {
        File file = new File(context.getFilesDir(), USERS_FILE);
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(users.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) {
            // Keep auth non-crashing for the prototype; failed writes simply make the action retryable.
        }
    }

    private JSONObject toJson(User user) {
        return toJson(user, user.username());
    }

    private JSONObject toJson(User user, String displayName) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", user.id().toString());
            json.put("role", user.role().name());
            json.put("username", user.username());
            json.put("displayName", displayName);
            json.put("password", user.password());
            json.put("avatarSource", Avatar.SOURCE_DEFAULT);
            json.put("avatarValue", Avatar.DEFAULT_VALUE);
            json.put("profileBackgroundSource", ProfileBackground.SOURCE_DEFAULT);
            json.put("profileBackgroundValue", ProfileBackground.DEFAULT_VALUE);
        } catch (JSONException ignored) {
        }
        return json;
    }

    private String toDisplayName(String username) {
        if (username == null || username.isEmpty()) return "";
        return username.substring(0, 1).toUpperCase() + username.substring(1);
    }

    private User fromJson(JSONObject json) {
        if (json == null) return null;
        try {
            return new User(
                    UUID.fromString(json.getString("id")),
                    User.Role.valueOf(json.getString("role")),
                    json.getString("username"),
                    json.getString("password")
            );
        } catch (IllegalArgumentException | JSONException ignored) {
            return null;
        }
    }

    public record Avatar(String source, String value) {
        public static final String SOURCE_DEFAULT = "default";
        public static final String DEFAULT_VALUE = "avatar_default_1";

        public static Avatar defaultAvatar() {
            return new Avatar(SOURCE_DEFAULT, DEFAULT_VALUE);
        }
    }

    public record ProfileBackground(String source, String value) {
        public static final String SOURCE_DEFAULT = "default";
        public static final String DEFAULT_VALUE = "profile_background_default_1";

        public static ProfileBackground defaultBackground() {
            return new ProfileBackground(SOURCE_DEFAULT, DEFAULT_VALUE);
        }
    }
}

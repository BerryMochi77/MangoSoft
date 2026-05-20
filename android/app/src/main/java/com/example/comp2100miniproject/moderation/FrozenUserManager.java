package com.example.comp2100miniproject.moderation;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class FrozenUserManager {
    private static final String FROZEN_USERS_FILE = "frozen_report_users.json";

    private final Context context;

    public FrozenUserManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isFrozen(UUID userId) {
        if (userId == null) return false;
        JSONArray users = readFrozenUsers();
        for (int i = 0; i < users.length(); i++) {
            if (userId.toString().equals(users.optString(i))) return true;
        }
        return false;
    }

    public void freeze(UUID userId) {
        if (userId == null || isFrozen(userId)) return;
        JSONArray users = readFrozenUsers();
        users.put(userId.toString());
        writeFrozenUsers(users);
    }

    public void unfreeze(UUID userId) {
        if (userId == null) return;
        JSONArray users = readFrozenUsers();
        JSONArray result = new JSONArray();
        for (int i = 0; i < users.length(); i++) {
            String storedUserId = users.optString(i);
            if (!userId.toString().equals(storedUserId)) result.put(storedUserId);
        }
        writeFrozenUsers(result);
    }

    public List<UUID> getFrozenUserIds() {
        ArrayList<UUID> result = new ArrayList<>();
        JSONArray users = readFrozenUsers();
        for (int i = 0; i < users.length(); i++) {
            try {
                result.add(UUID.fromString(users.optString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private JSONArray readFrozenUsers() {
        File file = new File(context.getFilesDir(), FROZEN_USERS_FILE);
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

    private void writeFrozenUsers(JSONArray users) {
        File file = new File(context.getFilesDir(), FROZEN_USERS_FILE);
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(users.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException ignored) {
        }
    }
}

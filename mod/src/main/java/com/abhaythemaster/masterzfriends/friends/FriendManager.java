package com.abhaythemaster.masterzfriends.friends;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendManager {
    private static final String FILE = "config/masterzfriends_friends.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<Friend> friends = new CopyOnWriteArrayList<>();

    public FriendManager() { load(); }

    public void addFriend(Friend f) {
        if (getFriendById(f.discordId) == null) { friends.add(f); save(); }
    }

    public void removeFriend(String id) { friends.removeIf(f -> f.discordId.equals(id)); save(); }

    public Friend getFriendById(String id) {
        return friends.stream().filter(f -> f.discordId.equals(id)).findFirst().orElse(null);
    }

    public List<Friend> getAllFriends() { return Collections.unmodifiableList(friends); }

    public void updateStatus(String id, String status) {
        Friend f = getFriendById(id);
        if (f != null) f.status = status;
    }

    public void syncFromServer(JsonArray arr) {
        friends.clear();
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            friends.add(new Friend(
                o.get("discord_id").getAsString(),
                o.get("username").getAsString(),
                o.has("status") ? o.get("status").getAsString() : "offline"));
        }
        save();
    }

    private void save() {
        try {
            JsonArray arr = new JsonArray();
            for (Friend f : friends) {
                JsonObject o = new JsonObject();
                o.addProperty("discord_id", f.discordId);
                o.addProperty("username", f.username);
                o.addProperty("status", f.status);
                arr.add(o);
            }
            Files.createDirectories(Path.of("config"));
            Files.writeString(Path.of(FILE), GSON.toJson(arr));
        } catch (IOException e) { MasterzFriends.LOGGER.error("Save failed: " + e.getMessage()); }
    }

    private void load() {
        try {
            Path p = Path.of(FILE);
            if (!Files.exists(p)) return;
            JsonArray arr = GSON.fromJson(Files.readString(p), JsonArray.class);
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                friends.add(new Friend(
                    o.get("discord_id").getAsString(),
                    o.get("username").getAsString(), "offline"));
            }
        } catch (Exception e) { MasterzFriends.LOGGER.error("Load failed: " + e.getMessage()); }
    }

    public static class Friend {
        public String discordId, username;
        public volatile String status;
        public Friend(String discordId, String username, String status) {
            this.discordId = discordId; this.username = username; this.status = status;
        }
        public boolean isOnline() { return "online".equals(status); }
    }
}

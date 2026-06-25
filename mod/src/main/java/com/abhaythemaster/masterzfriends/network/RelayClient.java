package com.abhaythemaster.masterzfriends.network;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import com.abhaythemaster.masterzfriends.friends.FriendManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class RelayClient {
    private static final Gson GSON = new Gson();
    private WebSocket ws;
    private boolean connected = false;
    private Consumer<JsonObject> dmCallback;

    public void onDM(Consumer<JsonObject> cb) { this.dmCallback = cb; }

    public void connect(String token) {
        if (connected) return;
        try {
            ws = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(MasterzFriends.RELAY_WS), new WSListener())
                .get(10, TimeUnit.SECONDS);
            JsonObject auth = new JsonObject();
            auth.addProperty("type", "auth");
            auth.addProperty("token", token);
            send(auth);
        } catch (Exception e) {
            MasterzFriends.LOGGER.error("Relay connect failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        connected = false;
    }

    public void sendDM(String toId, String message) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "dm");
        p.addProperty("to", toId);
        p.addProperty("message", message);
        send(p);
    }

    public void sendFriendRequest(String toId) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "friend_request");
        p.addProperty("to", toId);
        send(p);
    }

    public void acceptFriendRequest(String fromId) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "friend_accept");
        p.addProperty("to", fromId);
        send(p);
    }

    public void removeFriend(String id) {
        JsonObject p = new JsonObject();
        p.addProperty("type", "friend_remove");
        p.addProperty("to", id);
        send(p);
    }

    private void send(JsonObject obj) {
        if (ws != null) ws.sendText(GSON.toJson(obj), true);
    }

    private void handlePacket(String raw) {
        try {
            JsonObject pkt = GSON.fromJson(raw, JsonObject.class);
            String type = pkt.get("type").getAsString();
            switch (type) {
                case "auth_ok" -> {
                    connected = true;
                    MasterzFriends.LOGGER.info("[MasterzFriends] Relay connected!");
                    if (pkt.has("friends"))
                        MasterzFriends.friendManager.syncFromServer(pkt.getAsJsonArray("friends"));
                }
                case "dm" -> {
                    String from = pkt.get("from_username").getAsString();
                    String msg = pkt.get("message").getAsString();
                    if (dmCallback != null) dmCallback.accept(pkt);
                    MinecraftClient.getInstance().execute(() -> {
                        var hud = MinecraftClient.getInstance().inGameHud;
                        if (hud != null)
                            hud.getChatHud().addMessage(
                                Text.literal("§b[MZF DM] §e" + from + " §7» §f" + msg));
                    });
                }
                case "friend_request" -> {
                    String from = pkt.get("from_username").getAsString();
                    String fromId = pkt.get("from_id").getAsString();
                    MinecraftClient.getInstance().execute(() -> {
                        var hud = MinecraftClient.getInstance().inGameHud;
                        if (hud != null)
                            hud.getChatHud().addMessage(
                                Text.literal("§b[MZF] §e" + from + " §fne friend request bheja! §7/mzf friend accept " + fromId));
                    });
                }
                case "friend_accepted" -> {
                    String from = pkt.get("from_username").getAsString();
                    String fromId = pkt.get("from_id").getAsString();
                    MasterzFriends.friendManager.addFriend(
                        new FriendManager.Friend(fromId, from, "online"));
                    MinecraftClient.getInstance().execute(() -> {
                        var hud = MinecraftClient.getInstance().inGameHud;
                        if (hud != null)
                            hud.getChatHud().addMessage(
                                Text.literal("§b[MZF] §a" + from + " ne request accept kar li!"));
                    });
                }
                case "friend_list" ->
                    MasterzFriends.friendManager.syncFromServer(pkt.getAsJsonArray("friends"));
                case "status_update" ->
                    MasterzFriends.friendManager.updateStatus(
                        pkt.get("discord_id").getAsString(),
                        pkt.get("status").getAsString());
            }
        } catch (Exception e) {
            MasterzFriends.LOGGER.error("Packet error: " + e.getMessage());
        }
    }

    public boolean isConnected() { return connected; }

    private class WSListener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) { handlePacket(buf.toString()); buf.setLength(0); }
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
            connected = false;
            return WebSocket.Listener.super.onClose(ws, code, reason);
        }

        @Override
        public void onError(WebSocket ws, Throwable e) {
            connected = false;
            MasterzFriends.LOGGER.error("WS error: " + e.getMessage());
        }
    }
}

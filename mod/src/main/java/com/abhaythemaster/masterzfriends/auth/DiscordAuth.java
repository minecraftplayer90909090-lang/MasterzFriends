package com.abhaythemaster.masterzfriends.auth;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class DiscordAuth {
    private static final String CONFIG = "config/masterzfriends_auth.json";
    private static final Gson GSON = new Gson();
    private String token, discordId, username;
    private boolean loggedIn = false;

    public DiscordAuth() { loadSaved(); }

    public CompletableFuture<Boolean> startLogin() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(MasterzFriends.RELAY_HTTP + "/auth/url")).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
                String authUrl = json.get("url").getAsString();
                String code = json.get("code").getAsString();

                try { Desktop.getDesktop().browse(URI.create(authUrl)); }
                catch (Exception e) { MasterzFriends.LOGGER.info("Open this URL: " + authUrl); }

                for (int i = 0; i < 120; i++) {
                    Thread.sleep(1000);
                    HttpRequest poll = HttpRequest.newBuilder()
                        .uri(URI.create(MasterzFriends.RELAY_HTTP + "/auth/token?code=" + code))
                        .GET().build();
                    HttpResponse<String> pr = client.send(poll, HttpResponse.BodyHandlers.ofString());
                    if (pr.statusCode() == 200) {
                        JsonObject d = GSON.fromJson(pr.body(), JsonObject.class);
                        if (d.has("token")) {
                            token = d.get("token").getAsString();
                            discordId = d.get("discord_id").getAsString();
                            username = d.get("username").getAsString();
                            loggedIn = true;
                            saveAuth();
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                MasterzFriends.LOGGER.error("Login failed: " + e.getMessage());
            }
            return false;
        });
    }

    public void logout() {
        token = discordId = username = null;
        loggedIn = false;
        try { Files.deleteIfExists(Path.of(CONFIG)); } catch (IOException ignored) {}
    }

    private void saveAuth() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("token", token);
            o.addProperty("discord_id", discordId);
            o.addProperty("username", username);
            Files.createDirectories(Path.of("config"));
            Files.writeString(Path.of(CONFIG), GSON.toJson(o));
        } catch (IOException e) { MasterzFriends.LOGGER.error("Save auth failed: " + e.getMessage()); }
    }

    private void loadSaved() {
        try {
            Path p = Path.of(CONFIG);
            if (!Files.exists(p)) return;
            JsonObject o = GSON.fromJson(Files.readString(p), JsonObject.class);
            token = o.get("token").getAsString();
            discordId = o.get("discord_id").getAsString();
            username = o.get("username").getAsString();
            loggedIn = true;
            MasterzFriends.LOGGER.info("[MasterzFriends] Auto-login as " + username);
        } catch (Exception ignored) {}
    }

    public boolean isLoggedIn() { return loggedIn; }
    public String getToken() { return token; }
    public String getDiscordId() { return discordId; }
    public String getUsername() { return username; }
}

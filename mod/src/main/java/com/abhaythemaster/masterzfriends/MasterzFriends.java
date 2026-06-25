package com.abhaythemaster.masterzfriends;

import com.abhaythemaster.masterzfriends.auth.DiscordAuth;
import com.abhaythemaster.masterzfriends.command.FriendCommand;
import com.abhaythemaster.masterzfriends.friends.FriendManager;
import com.abhaythemaster.masterzfriends.network.RelayClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterzFriends implements ClientModInitializer {
    public static final String MOD_ID = "masterzfriends";
    public static final Logger LOGGER = LoggerFactory.getLogger("Masterz Friends");
    public static final String RELAY_WS = "wss://masterzfriends-production.up.railway.app";
    public static final String RELAY_HTTP = "https://masterzfriends-production.up.railway.app";

    public static RelayClient relayClient;
    public static FriendManager friendManager;
    public static DiscordAuth discordAuth;
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[MasterzFriends] Starting...");
        friendManager = new FriendManager();
        discordAuth = new DiscordAuth();
        relayClient = new RelayClient();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.masterzfriends.open", InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F, "category.masterzfriends"));

        ClientCommandRegistrationCallback.EVENT.register((d, r) -> FriendCommand.register(d));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null)
                    client.setScreen(new com.abhaythemaster.masterzfriends.gui.FriendListScreen(null));
            }
            if (discordAuth.isLoggedIn() && !relayClient.isConnected())
                relayClient.connect(discordAuth.getToken());
        });
        LOGGER.info("[MasterzFriends] Ready!");
    }

    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }
}

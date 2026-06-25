package com.abhaythemaster.masterzfriends.gui;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import com.abhaythemaster.masterzfriends.friends.FriendManager;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DMScreen extends Screen {
    private final Screen parent;
    private final FriendManager.Friend friend;
    private TextFieldWidget input;
    private final List<String[]> messages = new ArrayList<>();
    private int scroll = 0;

    public DMScreen(Screen parent, FriendManager.Friend friend) {
        super(Text.literal("DM - " + friend.username));
        this.friend = friend;
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("§7←"), btn ->
            client.setScreen(parent)).dimensions(8, 8, 24, 18).build());
        input = new TextFieldWidget(textRenderer, width / 2 - 130, height - 26, 240, 18, Text.literal(""));
        input.setPlaceholder(Text.literal("§7Message likho..."));
        input.setMaxLength(256);
        addDrawableChild(input);
        setInitialFocus(input);
        addDrawableChild(ButtonWidget.builder(Text.literal("§bSend"), btn -> send())
            .dimensions(width / 2 + 116, height - 26, 40, 18).build());

        Consumer<JsonObject> dmHandler = pkt -> {
            if (pkt.get("from_id").getAsString().equals(friend.discordId)) {
                messages.add(new String[]{friend.username, pkt.get("message").getAsString()});
            }
        };
        MasterzFriends.relayClient.onDM(dmHandler);
    }

    private void send() {
        String txt = input.getText().trim();
        if (txt.isEmpty()) return;
        MasterzFriends.relayClient.sendDM(friend.discordId, txt);
        messages.add(new String[]{"You", txt});
        input.setText("");
        scroll = Math.max(0, messages.size() * 13 - (height - 80));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xCC0D1A2E);
        int px = width / 2 - 160, pw = 320;
        ctx.fill(px, 0, px + pw, height, 0x881A3A5C);
        ctx.fill(px, 0, px + pw, 2, 0xFF00E5FF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§b" + friend.username + " §7— DM"), width / 2, 10, 0xFF00E5FF);
        ctx.fill(px + 4, 28, px + pw - 4, height - 32, 0xAA0A1A30);
        int y = 32 - scroll;
        for (String[] m : messages) {
            if (y > 28 && y < height - 32) {
                boolean own = "You".equals(m[0]);
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal((own ? "§b[You] §f" : "§e[" + m[0] + "] §f") + m[1]),
                    px + 8, y, 0xFFFFFFFF);
            }
            y += 13;
        }
        ctx.fill(px + 4, height - 29, px + pw - 4, height - 7, 0xAA0A1A30);
        ctx.fill(px + 4, height - 30, px + pw - 4, height - 29, 0xFF00E5FF);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 257 || k == 335) { send(); return true; }
        return super.keyPressed(k, s, m);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8)); return true;
    }

    @Override public boolean shouldPause() { return false; }
}

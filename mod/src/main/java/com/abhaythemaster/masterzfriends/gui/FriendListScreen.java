package com.abhaythemaster.masterzfriends.gui;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import com.abhaythemaster.masterzfriends.friends.FriendManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.List;

public class FriendListScreen extends Screen {
    private final Screen parent;
    private int scroll = 0, selected = -1;
    private static final int CARD_H = 28, MARGIN = 4, TOP = 55;
    private static final int C_BG = 0xCC0D1A2E, C_CARD = 0x881A3A5C;
    private static final int C_BORDER = 0xFF00E5FF, C_ONLINE = 0xFF00FF88;
    private static final int C_OFFLINE = 0xFF666666, C_TEXT = 0xFFFFFFFF;
    private static final int C_SUB = 0xFFAAAAAA;

    public FriendListScreen(Screen parent) {
        super(Text.literal("Masterz Friends"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (!MasterzFriends.discordAuth.isLoggedIn()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("§b Login with Discord"), btn -> {
                btn.setMessage(Text.literal("§7Opening..."));
                MasterzFriends.discordAuth.startLogin().thenAccept(ok -> {
                    if (ok) MasterzFriends.relayClient.connect(MasterzFriends.discordAuth.getToken());
                });
            }).dimensions(width / 2 - 80, height / 2 - 12, 160, 24).build());
            return;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("§b+ Add"), btn ->
            client.setScreen(new AddFriendScreen(this)))
            .dimensions(width - 60, 8, 52, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§7✕"), btn ->
            client.setScreen(parent))
            .dimensions(8, 8, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§bMsg"), btn -> {
            if (selected >= 0) {
                List<FriendManager.Friend> f = MasterzFriends.friendManager.getAllFriends();
                if (selected < f.size()) client.setScreen(new DMScreen(this, f.get(selected)));
            }
        }).dimensions(width / 2 - 55, height - 26, 50, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§cRemove"), btn -> {
            if (selected >= 0) {
                List<FriendManager.Friend> f = MasterzFriends.friendManager.getAllFriends();
                if (selected < f.size()) {
                    MasterzFriends.friendManager.removeFriend(f.get(selected).discordId);
                    MasterzFriends.relayClient.removeFriend(f.get(selected).discordId);
                    selected = -1;
                }
            }
        }).dimensions(width / 2 + 3, height - 26, 52, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, C_BG);
        int px = width / 2 - 120, pw = 240;
        ctx.fill(px, 0, px + pw, height, 0x441A2A4A);
        ctx.fill(px, 0, px + pw, 2, C_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b⚡ Masterz Friends"), width / 2, 12, C_BORDER);

        if (!MasterzFriends.discordAuth.isLoggedIn()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Discord se login karo"), width / 2, 36, C_SUB);
            super.render(ctx, mx, my, delta); return;
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7" + MasterzFriends.discordAuth.getUsername()), width / 2, 30, C_SUB);

        List<FriendManager.Friend> all = MasterzFriends.friendManager.getAllFriends();
        int y = TOP - scroll;
        for (int i = 0; i < all.size(); i++) {
            if (y + CARD_H > TOP && y < height - 35) {
                FriendManager.Friend f = all.get(i);
                boolean sel = i == selected;
                boolean hov = mx >= px + 4 && mx <= px + pw - 4 && my >= y && my <= y + CARD_H;
                ctx.fill(px + 4, y, px + pw - 4, y + CARD_H, sel || hov ? 0xAA1E4A70 : C_CARD);
                ctx.fill(px + 4, y, px + 6, y + CARD_H, C_BORDER);
                ctx.fill(px + 10, y + 10, px + 16, y + 18, f.isOnline() ? C_ONLINE : C_OFFLINE);
                ctx.drawTextWithShadow(textRenderer, Text.literal("§f" + f.username), px + 22, y + 6, C_TEXT);
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal(f.isOnline() ? "§aOnline" : "§7Offline"), px + 22, y + 16, C_SUB);
            }
            y += CARD_H + MARGIN;
        }
        if (all.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Koi friend nahi! §b+ Add§7 karo"), width / 2, TOP + 20, C_SUB);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int px = width / 2 - 116, pw = 232;
        int y = TOP - scroll;
        List<FriendManager.Friend> all = MasterzFriends.friendManager.getAllFriends();
        for (int i = 0; i < all.size(); i++) {
            if (mx >= px && mx <= px + pw && my >= y && my <= y + CARD_H) {
                selected = selected == i ? -1 : i; return true;
            }
            y += CARD_H + MARGIN;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, scroll - (int)(v * 8)); return true;
    }

    @Override public boolean shouldPause() { return false; }
}

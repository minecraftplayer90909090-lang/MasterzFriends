package com.abhaythemaster.masterzfriends.gui;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AddFriendScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget field;
    private String status = "";
    private int statusColor = 0xFFAAAAAA;

    public AddFriendScreen(Screen parent) {
        super(Text.literal("Add Friend"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        field = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20, Text.literal(""));
        field.setPlaceholder(Text.literal("§7Discord ID daalo..."));
        field.setMaxLength(32);
        addDrawableChild(field);
        setInitialFocus(field);
        addDrawableChild(ButtonWidget.builder(Text.literal("§bSend Request"), btn -> {
            String id = field.getText().trim();
            if (id.isEmpty()) { status = "§cID daalo pehle!"; statusColor = 0xFFFF4444; return; }
            MasterzFriends.relayClient.sendFriendRequest(id);
            status = "§aRequest bhej diya!"; statusColor = 0xFF00FF88;
            field.setText("");
        }).dimensions(width / 2 - 55, height / 2 + 16, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("§7Back"), btn ->
            client.setScreen(parent)).dimensions(width / 2 - 25, height / 2 + 42, 50, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xCC0D1A2E);
        int px = width / 2 - 130;
        ctx.fill(px, height / 2 - 50, px + 260, height / 2 + 76, 0x881A3A5C);
        ctx.fill(px, height / 2 - 50, px + 260, height / 2 - 48, 0xFF00E5FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§b+ Add Friend"), width / 2, height / 2 - 42, 0xFF00E5FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Friend ka Discord ID daalo"), width / 2, height / 2 - 28, 0xFFAAAAAA);
        if (!status.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height / 2 + 66, statusColor);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(k, s, m);
    }

    @Override public boolean shouldPause() { return false; }
}

package com.abhaythemaster.masterzfriends.command;

import com.abhaythemaster.masterzfriends.MasterzFriends;
import com.abhaythemaster.masterzfriends.friends.FriendManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import java.util.List;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class FriendCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("mzf")
            .then(literal("login").executes(ctx -> {
                if (MasterzFriends.discordAuth.isLoggedIn()) {
                    ctx.getSource().sendFeedback(Text.literal("§b[MZF] §aLogged in as §b" + MasterzFriends.discordAuth.getUsername()));
                } else {
                    ctx.getSource().sendFeedback(Text.literal("§b[MZF] §7Browser mein Discord login khul raha hai..."));
                    MasterzFriends.discordAuth.startLogin().thenAccept(ok -> {
                        if (ok) MasterzFriends.relayClient.connect(MasterzFriends.discordAuth.getToken());
                    });
                }
                return 1;
            }))
            .then(literal("logout").executes(ctx -> {
                MasterzFriends.discordAuth.logout();
                MasterzFriends.relayClient.disconnect();
                ctx.getSource().sendFeedback(Text.literal("§b[MZF] §7Logged out."));
                return 1;
            }))
            .then(literal("friend")
                .then(literal("add").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    MasterzFriends.relayClient.sendFriendRequest(id);
                    ctx.getSource().sendFeedback(Text.literal("§b[MZF] §fRequest bheja: §b" + id));
                    return 1;
                })))
                .then(literal("accept").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    MasterzFriends.relayClient.acceptFriendRequest(id);
                    ctx.getSource().sendFeedback(Text.literal("§b[MZF] §aAccepted: §b" + id));
                    return 1;
                })))
                .then(literal("remove").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    MasterzFriends.friendManager.removeFriend(id);
                    MasterzFriends.relayClient.removeFriend(id);
                    ctx.getSource().sendFeedback(Text.literal("§b[MZF] §cRemoved: §b" + id));
                    return 1;
                })))
                .then(literal("list").executes(ctx -> {
                    List<FriendManager.Friend> list = MasterzFriends.friendManager.getAllFriends();
                    if (list.isEmpty()) {
                        ctx.getSource().sendFeedback(Text.literal("§b[MZF] §7Koi friend nahi! /mzf friend add <discord_id>"));
                    } else {
                        ctx.getSource().sendFeedback(Text.literal("§b[MZF] §fFriends (" + list.size() + "):"));
                        for (FriendManager.Friend f : list)
                            ctx.getSource().sendFeedback(Text.literal(
                                "  " + (f.isOnline() ? "§a● " : "§7○ ") + "§f" + f.username + " §8(" + f.discordId + ")"));
                    }
                    return 1;
                }))
            )
            .then(literal("msg")
                .then(argument("id", StringArgumentType.word())
                    .then(argument("msg", StringArgumentType.greedyString()).executes(ctx -> {
                        String id = StringArgumentType.getString(ctx, "id");
                        String msg = StringArgumentType.getString(ctx, "msg");
                        MasterzFriends.relayClient.sendDM(id, msg);
                        ctx.getSource().sendFeedback(Text.literal("§b[MZF] §f→ §b" + id + "§7: §f" + msg));
                        return 1;
                    }))
                )
            )
        );
    }
}

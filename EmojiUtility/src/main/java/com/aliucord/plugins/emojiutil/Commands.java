/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.emojiutil;

import com.aliucord.Utils;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.CommandContext;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.api.message.MessageFlags;
import com.discord.api.message.MessageTypes;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.models.domain.NonceGenerator;
import com.discord.models.message.Message;
import com.discord.stores.StoreMessages;
import com.discord.stores.StoreStream;
import com.discord.utilities.message.LocalMessageCreatorsKt;
import com.discord.utilities.time.Clock;
import com.discord.utilities.time.ClockFactory;
import com.discord.utilities.user.UserUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class Commands {
    private static final Field flagsField;
    private static final Field typeField;
    private static final Field contentField;
    static {
        try {
            flagsField = Message.class.getDeclaredField("flags");
            flagsField.setAccessible(true);
            typeField = Message.class.getDeclaredField("type");
            typeField.setAccessible(true);
            contentField = Message.class.getDeclaredField("content");
            contentField.setAccessible(true);
        } catch (Throwable th) { throw new RuntimeException(th); }
    }
    public static void registerAll(CommandsAPI api) {
        registerDownload(api);
    }

    private static CommandsAPI.CommandResult replyError(String msg) {
        final var xEmoji = "❌ ";
        return new CommandsAPI.CommandResult(xEmoji + msg, null, false);
    }

    private static void updateMessage(CommandContext ctx, AtomicReference<Message> ref, String content) {
        var storeMessages = StoreStream.getMessages();

        if (ref.get() != null)
            StoreMessages.access$handleLocalMessageDelete(storeMessages, ref.get());

        Clock clock = ClockFactory.get();
        long id = NonceGenerator.computeNonce(clock);
        Message msg = LocalMessageCreatorsKt.createLocalApplicationCommandMessage(
                id,
                "download",
                ctx.getChannelId(),
                UserUtils.INSTANCE.synthesizeApiUser(ctx.getMe()),
                Utils.buildClyde("Emoji Downloader", null),
                false,
                false,
                id,
                clock
        );
        Class<Message> c = Message.class;
        try {
            flagsField.set(msg, MessageFlags.EPHEMERAL);
            typeField.set(msg, MessageTypes.LOCAL);
            contentField.set(msg, content);
        } catch (Throwable ignored) { }

        ref.set(msg);
        StoreMessages.access$handleLocalMessageCreate(storeMessages, msg);
    }

    private static void registerDownload(CommandsAPI api) {
        var emojiArg = Collections.singletonList(
                new ApplicationCommandOption(ApplicationCommandType.STRING, "emojis", "list of emojis to download", null, true, false, null, null)
        );

        var guildIdArg = Collections.singletonList(
                new ApplicationCommandOption(ApplicationCommandType.STRING, "guildId", "id of guild to download from", null, false, false, null, null)
        );

        var arguments = Arrays.asList(
                new ApplicationCommandOption(ApplicationCommandType.SUBCOMMAND, "server", "Download all emotes from the current server", null, false, false, null, guildIdArg),
                new ApplicationCommandOption(ApplicationCommandType.SUBCOMMAND, "all", "Downloads all emotes you have access too", null, false, false, null, null),
                new ApplicationCommandOption(ApplicationCommandType.SUBCOMMAND, "reply", "Download all emotes from the message this is in reply to", null, false, false, null, null),
                new ApplicationCommandOption(ApplicationCommandType.SUBCOMMAND, "provided", "Download all emotes passed as argument", null, false, false, null, emojiArg)
        );

        api.registerCommand("download", "Download emojis", arguments, ctx -> {
            var updateMsg = new AtomicReference<Message>();

            int[] ret;
            if (ctx.containsArg("server")) {
                long guildId;
                try {
                    //noinspection ConstantConditions
                    guildId = Long.parseLong((String) ctx.getSubCommandArgs("server").get("guildId"));
                } catch (NullPointerException | NumberFormatException ignored) {
                    if (!ctx.getChannel().isGuild()) return replyError("Please either specify a valid server id or run this from a server");
                    guildId = ctx.getChannel().getGuildId();
                }

                var guild = StoreStream.getGuilds().getGuild(guildId);
                if (guild == null) return replyError("No such guild");

                updateMessage(ctx, updateMsg, String.format("Downloading %s emojis from %s... This might take a while.", guild.getEmojis().size(), guild.getName()));
                ret = EmojiDownloader.downloadFromGuild(guild);
            } else if (ctx.containsArg("all")) {
                ret = EmojiDownloader.downloadFromAllGuilds((guildName, progress, amount, stats) -> {
                    var content = String.format(
                            "Downloading emojis from all guilds... Go grab a cup of tea or something as this will take a while.\n\n**Current Guild**: [%s] %s (%s emojis)\n**Download Stats**: ```\nSuccess: %s\nAlready downloaded: %s\nFailed: %s```",
                            progress,
                            guildName,
                            amount,
                            stats[0],
                            stats[1],
                            stats[2]
                    );
                    updateMessage(ctx, updateMsg, content);
                    return null;
                });
            } else if (ctx.containsArg("reply")) {
                var msg = ctx.getReferencedMessage();
                if (msg == null) return replyError("You need to reference a message lol");
                if (msg.getContent() == null) return replyError("That message has no content");
                ret = EmojiDownloader.downloadFromString(msg.getContent());
            } else if (ctx.containsArg("provided")) {
                ret = EmojiDownloader.downloadFromString((String) ctx.getSubCommandArgs("provided").get("emojis"));
            } else {
                return new CommandsAPI.CommandResult("Please choose a subcommand!", null, false);
            }

            if (ret == null || (ret[0] == 0 && ret[1] == 0 && ret[2] == 0)) return replyError("No emojis specified");
            updateMessage(ctx, updateMsg, String.format("Done! Stats: ```\nSuccess: %s\nAlready downloaded: %s\nFailed: %s```", ret[0], ret[1], ret[2]));
            return null;
        });
    }
}

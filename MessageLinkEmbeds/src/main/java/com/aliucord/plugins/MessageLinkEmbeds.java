/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.CollectionUtils;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.discord.api.utcdatetime.UtcDateTime;
import com.discord.stores.StoreStream;
import com.discord.utilities.SnowflakeUtils;
import com.discord.utilities.icon.IconUtils;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class MessageLinkEmbeds extends Plugin {
    private static final Pattern messageLinkPattern = Pattern.compile("https?://((canary|ptb)\\.)?discord(app)?\\.com/channels/(\\d{17,19}|@me)/(\\d{17,19})/(\\d{17,19})");

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    private static final String className = "com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage";
    @Override
    public void start(Context context) {
        var msgStore = StoreStream.getMessages();
        var channelStore = StoreStream.getChannels();
        var guildStore = StoreStream.getGuilds();

        patcher.patch(className, "onConfigure", new Class<?>[]{int.class, ChatListEntry.class}, new PinePatchFn(callFrame -> {
            var msg = ((MessageEntry) callFrame.args[1]).getMessage();
            if (msg == null) return;
            var embeds = msg.getEmbeds();
            var matcher = messageLinkPattern.matcher(msg.getContent());
            while (matcher.find()) {
                String url = matcher.group();
                String messageIdStr = matcher.group(6);
                String channelIdStr = matcher.group(5);
                if (messageIdStr == null || channelIdStr == null) continue; // Please shut up java
                long channelId = Long.parseLong(channelIdStr);
                long messageId = Long.parseLong(messageIdStr);
                // Check if link already embedded by checking if footer starting with # exists
                if (CollectionUtils.find(embeds, e -> e.e() != null && e.e().b().startsWith("#")) != null) continue;

                var m = msgStore.getMessage(channelId, messageId);
                if (m == null) return;
                var author = m.getAuthor();
                String avatarUrl = IconUtils.getForUser(author.f(), author.a().a(), Integer.valueOf(author.c()), true);
                var eb = new MessageEmbedBuilder()
                        .setUrl(url)
                        .setAuthor(author.o(), avatarUrl, avatarUrl)
                        .setDescription(m.getContent())
                        .setTimestamp(new UtcDateTime(SnowflakeUtils.toTimestamp(messageId)));

                var mEmbeds = m.getEmbeds();
                if (mEmbeds.size() != 0) {
                    mEmbeds.size();
                    var color = CollectionUtils.find(mEmbeds, e -> e.b() != null);
                    if (color != null) eb.setColor(color.b());
                    var img = CollectionUtils.find(mEmbeds, e -> e.f() != null);
                    if (img != null) eb.setImage(img.f().c(), img.f().b());
                    if (m.getContent().equals("")) {
                        var description = CollectionUtils.find(mEmbeds, e -> e.c() != null);
                        if (description != null) eb.setDescription(description.c());
                    }
                }

                var attachments = m.getAttachments();
                if (attachments.size() != 0) {
                    var firstAtachment = attachments.get(0);
                    eb.setImage(firstAtachment.f(), firstAtachment.c(), firstAtachment.b(), firstAtachment.g());
                }

                var channel = channelStore.getChannel(channelId);
                if (channel != null) {
                    var guild = guildStore.getGuild(channel.e());
                    if (guild != null) eb.setFooter(String.format("#%s (%s)", channel.l(), guild.getName()), null, null);
                }

                embeds.add(eb.build());
            }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}

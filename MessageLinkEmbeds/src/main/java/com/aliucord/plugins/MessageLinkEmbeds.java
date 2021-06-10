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
import com.aliucord.Logger;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.discord.api.message.embed.MessageEmbed;
import com.discord.api.utcdatetime.UtcDateTime;
import com.discord.models.domain.ModelMessage;
import com.discord.stores.StoreStream;
import com.discord.utilities.SnowflakeUtils;
import com.discord.utilities.icon.IconUtils;
import com.discord.utilities.rest.RestAPI;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import rx.Subscriber;

@SuppressWarnings("unused")
public class MessageLinkEmbeds extends Plugin {
    private static final Pattern messageLinkPattern = Pattern.compile("https?://((canary|ptb)\\.)?discord(app)?\\.com/channels/(\\d{17,19}|@me)/(\\d{17,19})/(\\d{17,19})");
    private static final Logger logger = new Logger("MessageLinkEmbeds");
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Map<Long, ModelMessage> cache = new HashMap<>();

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Embeds message links";
        manifest.version = "1.0.1";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    private static final String className = "com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage";
    @Override
    public void start(Context context) {
        var msgStore = StoreStream.getMessages();

        patcher.patch(className, "onConfigure", new Class<?>[]{int.class, ChatListEntry.class}, new PinePatchFn(callFrame -> {
            var msg = ((MessageEntry) callFrame.args[1]).getMessage();
            if (msg == null) return;
            var embeds = msg.getEmbeds();
            var matcher = messageLinkPattern.matcher(msg.getContent());
            while (matcher.find()) {
                String url = matcher.group();
                // Check if link already embedded by checking if embed with same url already exists
                if (CollectionUtils.find(embeds, e -> e.l() != null && e.l().equals(url)) != null) continue;

                String messageIdStr = matcher.group(6);
                String channelIdStr = matcher.group(5);
                if (messageIdStr == null || channelIdStr == null) continue; // Please shut up java
                long channelId = Long.parseLong(channelIdStr);
                long messageId = Long.parseLong(messageIdStr);

                var m = cache.get(messageId);
                if (m == null) m = msgStore.getMessage(channelId, messageId);
                if (m != null) {
                    addEmbed(embeds, m, url, messageId, channelId);
                } else {
                    Runnable doFetch = () -> {
                        var api = RestAPI.getApi();
                        if (api == null) return;
                        var observable = api.getChannelMessagesAround(channelId, 1, messageId);
                        if (observable == null) return;
                        observable.V(new Subscriber<>() {
                            public void onCompleted() { }
                            public void onError(Throwable th) {
                                logger.error(th);
                            }
                            public void onNext(List<ModelMessage> messages) {
                                if (messages.size() == 0) return;
                                var msg = messages.get(0);
                                cache.put(messageId, msg);
                                addEmbed(embeds, msg, url, messageId, channelId);
                                // TODO: Force re-render
                            }
                        });
                    };
                    worker.execute(doFetch);
                }
            }
        }));
    }

    public void addEmbed(List<MessageEmbed> embeds, ModelMessage msg, String url, long messageId, long channelId) {
        var author = msg.getAuthor();
        String avatar = author.a().a();
        String avatarUrl = IconUtils.getForUser(author.i(), avatar, Integer.valueOf(author.f()), avatar != null && avatar.startsWith("a_"));
        var eb = new MessageEmbedBuilder()
                .setUrl(url)
                .setAuthor(author.r() + "#" + author.f(), avatarUrl, avatarUrl)
                .setDescription(msg.getContent())
                .setTimestamp(new UtcDateTime(SnowflakeUtils.toTimestamp(messageId)));

        var mEmbeds = msg.getEmbeds();
        if (mEmbeds.size() != 0) {
            var color = CollectionUtils.find(mEmbeds, e -> e.b() != null);
            if (color != null) eb.setColor(color.b());
            var img = CollectionUtils.find(mEmbeds, e -> e.f() != null);
            if (img != null) {
                eb.setImage(img.f().c(), img.f().b(), img.f().a(), img.f().d());
            } else if ((img = CollectionUtils.find(mEmbeds, e -> e.h() != null)) != null) {
                eb.setImage(img.h().c(), img.h().b(), img.h().a(), img.h().d());
            }
            if (msg.getContent().equals("")) {
                var description = CollectionUtils.find(mEmbeds, e -> e.c() != null);
                if (description != null) eb.setDescription(description.c());
            }
        }

        var attachments = msg.getAttachments();
        if (attachments.size() != 0) {
            var firstAttachment = attachments.get(0);
            eb.setImage(firstAttachment.f(), firstAttachment.c(), firstAttachment.b(), firstAttachment.g());
        }

        var channel = StoreStream.getChannels().getChannel(channelId);
        if (channel != null) {
            var guild = StoreStream.getGuilds().getGuild(channel.e());
            if (guild != null)
                eb.setFooter(String.format("#%s (%s)", channel.l(), guild.getName()), null, null);
        }

        embeds.add(eb.build());
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}

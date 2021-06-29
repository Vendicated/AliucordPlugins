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
import android.os.Handler;

import androidx.annotation.NonNull;

import com.airbnb.lottie.parser.AnimatableValueParser;
import com.aliucord.entities.Plugin;
import com.aliucord.wrappers.messages.MessageWrapper;
import com.discord.api.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.utilities.message.MessageUtils;
import com.discord.utilities.rx.ObservableExtensionsKt;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$editMessage$1;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$editMessage$2;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodReplacement;

@SuppressWarnings("unused")
public class TapTap extends Plugin {
    private static final Handler handler = new Handler();
    private boolean busy = false;
    private int clicks = 0;

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Double tap someone else's message to quick reply, double tap your own to quick edit";
        manifest.version = "1.0.1";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context ctx) {
        patcher.patch("com.discord.widgets.chat.list.adapter.WidgetChatListAdapterEventsHandler", "onMessageClicked", new Class<?>[] { Message.class, boolean.class }, new MethodReplacement() {
            @Override
            protected Object replaceCall(Pine.CallFrame callFrame) {
                if (busy) return null;
                busy = true;
                var msg = (Message) callFrame.args[0];
                clicks++;
                handler.postDelayed(() -> {
                    if (clicks >= 2) {
                        long myId = StoreStream.getUsers().getMe().getId();
                        long id = new CoreUser(MessageWrapper.getAuthor(msg)).getId();
                        if (id == myId) {
                            editMessage(msg);
                        } else {
                            replyMessage(msg);
                        }
                    } else {
                        if ((boolean) callFrame.args[1]) StoreStream.Companion.getMessagesLoader().jumpToMessage(MessageWrapper.getChannelId(msg), MessageWrapper.getId(msg));
                    }
                    clicks = 0;
                }, 200);
                busy = false;
                return null;
            }
        });
    }

    /** WidgetChatListActions.replyMessage */
    private synchronized void replyMessage(Message msg) {
        var channel = StoreStream.getChannels().getChannel(MessageWrapper.getChannelId(msg));
        boolean isPrivateChannel = AnimatableValueParser.r1(channel);
        boolean isWebhook = MessageUtils.INSTANCE.isWebhook(msg);
        boolean shouldMention = !isWebhook;
        boolean shouldShowMentionToggle = !(isPrivateChannel || isWebhook);
        StoreStream.Companion.getPendingReplies().onCreatePendingReply(channel, msg, shouldMention, shouldShowMentionToggle);
    }

    /** WidgetChatListActions.editMessage */
    @SuppressWarnings("rawtypes")
    private synchronized void editMessage(Message msg) {
        var obs = StoreStream.getChannels().observeGuildAndPrivateChannels().Z(new WidgetChatListActions$editMessage$1<>(msg));
        // how come discord can pass null for these but i cant...
        Function1 doNothing = o -> null;
        Function0 doNothing2ElectricBoogaloo = () -> null;
        ObservableExtensionsKt.appSubscribe$default(
            ObservableExtensionsKt.takeSingleUntilTimeout$default(ObservableExtensionsKt.computationBuffered(obs), 0, false, 3, null),
            null,
            "editMessage",
            doNothing,
            new WidgetChatListActions$editMessage$2(msg),
            doNothing,
            doNothing2ElectricBoogaloo,
            doNothing2ElectricBoogaloo,
            177,
            null);
    }

    @Override
    public void stop(Context ctx) {
        patcher.unpatchAll();
    }
}

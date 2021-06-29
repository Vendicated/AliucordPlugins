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
import android.view.View;

import androidx.annotation.NonNull;

import com.airbnb.lottie.parser.AnimatableValueParser;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.wrappers.messages.MessageWrapper;
import com.discord.api.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.utilities.message.MessageUtils;
import com.discord.utilities.rx.ObservableExtensionsKt;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$editMessage$1;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$editMessage$2;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

@SuppressWarnings("unused")
public class DoubleTap extends Plugin {
    private static final Handler handler = new Handler();

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

    private static class DoubleClickListener implements View.OnClickListener {
        private final Runnable onClick;
        private boolean busy = false;
        private int clicks = 0;

        public DoubleClickListener(Runnable onClickListener) {
            onClick = onClickListener;
        }

        public void onClick(View view) {
            if (busy) return;
            busy = true;
            clicks++;
            handler.postDelayed(() -> {
                if (clicks >= 2) {
                    onClick.run();
                }
                clicks = 0;
            }, 200);
            busy = false;
        }
    }

    @Override
    public void start(Context ctx) {
        patcher.patch(WidgetChatListAdapterItemMessage.class, "onConfigure", new Class<?>[]{ int.class, ChatListEntry.class }, new PinePatchFn(callFrame -> {
            var _this = (WidgetChatListAdapterItemMessage) callFrame.thisObject;
            var msg = ((MessageEntry) callFrame.args[1]).getMessage();
            if (msg == null) return;
            _this.itemView.setOnClickListener(new DoubleClickListener(() -> {
                long myId = StoreStream.getUsers().getMe().getId();
                long id = new CoreUser(MessageWrapper.getAuthor(msg)).getId();
                if (id == myId) {
                    editMessage(msg);
                } else {
                    replyMessage(msg);
                }
            }));
        }));
    }

    /** WidgetChatListActions.replyMessage */
    private void replyMessage(Message msg) {
        var channel = StoreStream.getChannels().getChannel(MessageWrapper.getChannelId(msg));
        boolean isPrivateChannel = AnimatableValueParser.r1(channel);
        boolean isWebhook = MessageUtils.INSTANCE.isWebhook(msg);
        boolean shouldMention = !isWebhook;
        boolean shouldShowMentionToggle = !(isPrivateChannel || isWebhook);
        StoreStream.Companion.getPendingReplies().onCreatePendingReply(channel, msg, shouldMention, shouldShowMentionToggle);
    }

    /** WidgetChatListActions.editMessage */
    @SuppressWarnings("rawtypes")
    private void editMessage(Message msg) {
        var obs = StoreStream.getChannels().observeGuildAndPrivateChannels().Z(new WidgetChatListActions$editMessage$1<>(msg));
        // how come discord can pass null for these but i cant...
        Function1 doNothing = o -> null;
        Function0 alsoDoNothing = () -> null;
        ObservableExtensionsKt.appSubscribe$default(
                ObservableExtensionsKt.takeSingleUntilTimeout$default(ObservableExtensionsKt.computationBuffered(obs), 0, false, 3, null),
                (Context) null,
                "editMessage",
                doNothing,
                new WidgetChatListActions$editMessage$2(msg),
                doNothing,
                alsoDoNothing,
                alsoDoNothing,
                177,
                (Object) null);
    }

    @Override
    public void stop(Context ctx) {
        patcher.unpatchAll();
    }
}

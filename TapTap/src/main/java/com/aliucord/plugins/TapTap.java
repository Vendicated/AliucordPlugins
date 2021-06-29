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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.airbnb.lottie.parser.AnimatableValueParser;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.views.Button;
import com.aliucord.views.Divider;
import com.aliucord.views.TextInput;
import com.aliucord.wrappers.messages.MessageWrapper;
import com.discord.api.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.utilities.message.MessageUtils;
import com.discord.utilities.rx.ObservableExtensionsKt;
import com.discord.views.CheckedSetting;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$editMessage$1;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$editMessage$2;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodReplacement;

@SuppressWarnings("unused")
public class TapTap extends Plugin {
    private static SettingsAPI leSettings;

    public static class TapTapSettings extends SettingsPage {
        @SuppressLint("SetTextI18n")
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void onViewBound(View view) {
            super.onViewBound(view);
            setActionBarTitle("TapTap");

            var ctx = requireContext();
            var layout = (LinearLayout) ((NestedScrollView) ((CoordinatorLayout) view).getChildAt(1)).getChildAt(0);
            int p = Utils.getDefaultPadding();
            layout.setPadding(p, p, p, p);

            var input = new TextInput(ctx);
            input.setHint("Double Tap Window (in ms)");

            var editText = input.getEditText();
            assert editText != null;

            var button = new Button(ctx);
            button.setText("Save");
            button.setOnClickListener(v -> {
                String text = editText.getText().toString().replaceFirst("/+$", "");
                leSettings.setInt("doubleTapWindow", Integer.parseInt(text));
                Utils.showToast(ctx, "Saved!");
            });

            editText.setMaxLines(1);
            editText.setText(String.valueOf(leSettings.getInt("doubleTapWindow", 200)));
            editText.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                public void afterTextChanged(Editable s) {
                    try {
                        Integer.parseInt(s.toString());
                        button.setAlpha(1f);
                        button.setClickable(true);
                    } catch (Throwable ignored) {
                        button.setAlpha(0.5f);
                        button.setClickable(false);
                    }
                }
            });

            var checkbox = Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.CHECK,
                    "Hide Buttons",
                    "Hides reply & edit buttons in the message actions menu. The reply button will still be shown on your own messages."
            );
            checkbox.setChecked(leSettings.getBool("hideButtons", false));
            checkbox.setOnCheckedListener(checked -> {
                leSettings.setBool("hideButtons", checked);
            });

            layout.addView(input);
            layout.addView(button);
            layout.addView(new Divider(ctx));
            layout.addView(checkbox);
        }
    }

    private static final Handler handler = new Handler();
    private boolean busy = false;
    private int clicks = 0;

    public TapTap() {
        super();
        leSettings = sets;
        settings = new Settings(TapTapSettings.class);
    }

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Double tap someone else's message to quick reply, double tap your own to quick edit";
        manifest.version = "1.0.2";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context ctx) {
        final int editId = Utils.getResId("dialog_chat_actions_edit", "id");
        final int replyId = Utils.getResId("dialog_chat_actions_reply", "id");

        patcher.patch(WidgetChatListActions.class, "configureUI", new Class<?>[] {WidgetChatListActions.Model.class}, new PinePatchFn(callFrame -> {
            if (!sets.getBool("hideButtons", false)) return;
            var _this = (WidgetChatListActions) callFrame.thisObject;
            var rootView = (NestedScrollView) _this.requireView();
            var layout = (LinearLayout) rootView.getChildAt(0);
            var msg = ((WidgetChatListActions.Model) callFrame.args[0]).getMessage();
            if (isMe(msg)) {
                layout.findViewById(editId).setVisibility(View.GONE);
            } else {
                layout.findViewById(replyId).setVisibility(View.GONE);
            }
        }));

        patcher.patch("com.discord.widgets.chat.list.adapter.WidgetChatListAdapterEventsHandler", "onMessageClicked", new Class<?>[] { Message.class, boolean.class }, new MethodReplacement() {
            @Override
            protected Object replaceCall(Pine.CallFrame callFrame) {
                if (busy) return null;
                busy = true;
                var msg = (Message) callFrame.args[0];
                clicks++;
                handler.postDelayed(() -> {
                    if (clicks >= 2) {
                        if (isMe(msg)) {
                            editMessage(msg);
                        } else {
                            replyMessage(msg);
                        }
                    } else {
                        if ((boolean) callFrame.args[1]) StoreStream.Companion.getMessagesLoader().jumpToMessage(MessageWrapper.getChannelId(msg), MessageWrapper.getId(msg));
                    }
                    clicks = 0;
                }, sets.getInt("doubleTapWindow", 200));
                busy = false;
                return null;
            }
        });
    }

    private static boolean isMe(Message msg) {
        long authorId = new CoreUser(MessageWrapper.getAuthor(msg)).getId();
        long myId = StoreStream.getUsers().getMe().getId();
        return authorId == myId;
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

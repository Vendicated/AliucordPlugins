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
import android.text.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.views.TextInput;
import com.discord.models.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.google.android.material.card.MaterialCardView;
import com.lytefast.flexinput.R$b;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodReplacement;

public class TapTap extends Plugin {
    private static final int defaultDelay = 200;
    private Runnable unpatch;
    public Runnable togglePatch;

    public static class TapTapSettings extends SettingsPage {
        private final TapTap plugin;
        public TapTapSettings(TapTap plugin) {
            this.plugin = plugin;
        }

        @SuppressLint("SetTextI18n")
        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        public void onViewBound(View view) {
            super.onViewBound(view);
            setActionBarTitle("TapTap");

            var ctx = requireContext();

            var input = new TextInput(ctx);
            input.setHint("Double Tap Window (in ms)");

            var editText = input.getEditText();
            assert editText != null;

            editText.setMaxLines(1);
            editText.setText(String.valueOf(plugin.settings.getInt("doubleTapWindow", defaultDelay)));
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                public void afterTextChanged(Editable s) {
                    var str = s.toString();
                    int i;
                    try { i = Integer.parseInt(str);
                    } catch (NumberFormatException ignored) { i = defaultDelay; }
                    plugin.settings.setInt("doubleTapWindow", i);
                }
            });

            var card = new MaterialCardView(ctx);
            card.setRadius(Utils.getDefaultCardRadius());
            card.setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R$b.colorBackgroundSecondary));
            var params = new MaterialCardView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, Utils.getDefaultPadding(), 0, 0);
            card.setLayoutParams(params);


            var checkbox = Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.SWITCH,
                    "Hide Buttons",
                    "Hides reply & edit buttons in the message actions menu. The reply button will still be shown on your own messages."
            );
            checkbox.setChecked(plugin.settings.getBool("hideButtons", false));
            checkbox.setOnCheckedListener(checked -> {
                plugin.settings.setBool("hideButtons", checked);
                plugin.togglePatch.run();
            });

            card.addView(checkbox);

            addView(input);
            addView(card);
        }
    }

    public TapTap() {
        settingsTab = new SettingsTab(TapTapSettings.class).withArgs(this);
    }

    private WidgetChatListActions widgetChatListActions;
    private static final Handler handler = new Handler();
    private boolean busy = false;
    private int clicks = 0;

    @NonNull
    @Override
    public Manifest getManifest() {
        var manifest = new Manifest();
        manifest.authors = new Manifest.Author[] { new Manifest.Author("Vendicated", 343383572805058560L) };
        manifest.description = "Double tap someone else's message to quick reply, double tap your own to quick edit";
        manifest.version = "1.0.6";
        manifest.updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json";
        return manifest;
    }

    @Override
    public void start(Context ctx) {
        final int editId = Utils.getResId("dialog_chat_actions_edit", "id");
        final int replyId = Utils.getResId("dialog_chat_actions_reply", "id");

        Utils.mainThread.post(() -> widgetChatListActions = new WidgetChatListActions());

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
                            WidgetChatListActions.access$editMessage(widgetChatListActions, msg);
                        } else {
                            WidgetChatListActions.access$replyMessage(widgetChatListActions, msg, StoreStream.getChannels().getChannel(msg.getChannelId()));
                        }
                    } else {
                        if ((boolean) callFrame.args[1])
                            StoreStream.Companion.getMessagesLoader().jumpToMessage(msg.getChannelId(), msg.getId());
                    }
                    clicks = 0;
                }, settings.getInt("doubleTapWindow", defaultDelay));
                busy = false;
                return null;
            }
        });

        togglePatch = () -> {
            if (settings.getBool("hideButtons", false)) {
                if (unpatch == null)
                    unpatch = patcher.patch(WidgetChatListActions.class, "configureUI", new Class<?>[]{WidgetChatListActions.Model.class}, new PinePatchFn(callFrame -> {
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
            } else {
                if (unpatch != null) unpatch.run();
                unpatch = null;
            }
        };
        togglePatch.run();
    }

    private static boolean isMe(Message msg) {
        long authorId = new CoreUser(msg.getAuthor()).getId();
        long myId = StoreStream.getUsers().getMe().getId();
        return authorId == myId;
    }

    @Override
    public void stop(Context ctx) {
        patcher.unpatchAll();
    }
}

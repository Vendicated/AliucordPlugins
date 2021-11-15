/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.vendicated.aliucordplugs.taptap;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import androidx.core.widget.NestedScrollView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.discord.models.message.Message;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterEventsHandler;
import com.lytefast.flexinput.widget.FlexEditText;

@AliucordPlugin
public class TapTap extends Plugin {
    public static final int defaultDelay = 200;
    private Runnable unpatch;
    public Runnable togglePatch;

    public TapTap() {
        settingsTab = new SettingsTab(TapTapSettings.class).withArgs(this);
    }

    private WidgetChatListActions widgetChatListActions;
    private FlexEditText flexInput;
    private static final Handler handler = new Handler();
    private boolean busy = false;
    private int clicks = 0;

    @Override
    public void start(Context ctx) throws Throwable {
        final int editId = Utils.getResId("dialog_chat_actions_edit", "id");
        final int replyId = Utils.getResId("dialog_chat_actions_reply", "id");

        Utils.mainThread.post(() -> widgetChatListActions = new WidgetChatListActions());

        patcher.patch(FlexEditText.class.getDeclaredConstructor(Context.class, AttributeSet.class), new Hook(param -> flexInput = (FlexEditText) param.thisObject));

        patcher.patch(WidgetChatListAdapterEventsHandler.class.getDeclaredMethod("onMessageClicked", Message.class, boolean.class), new InsteadHook(param -> {
            if (busy) return null;
            busy = true;
            var msg = (Message) param.args[0];
            clicks++;
            handler.postDelayed(() -> {
                if (clicks >= 2) {
                    if (isMe(msg)) {
                        WidgetChatListActions.access$editMessage(widgetChatListActions, msg);
                    } else {
                        WidgetChatListActions.access$replyMessage(widgetChatListActions, msg, StoreStream.getChannels().getChannel(msg.getChannelId()));
                        if (settings.getBool("openKeyboard", false)) {
                            var imm = (InputMethodManager) Utils.getAppContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                            flexInput.requestFocus();
                        }
                    }
                } else {
                    if ((boolean) param.args[1])
                        StoreStream.Companion.getMessagesLoader().jumpToMessage(msg.getChannelId(), msg.getId());
                }
                clicks = 0;
            }, settings.getInt("doubleTapWindow", defaultDelay));
            busy = false;
            return null;
        }));

        togglePatch = () -> {
            if (settings.getBool("hideButtons", false)) {
                if (unpatch == null)
                    unpatch = patcher.patch(WidgetChatListActions.class, "configureUI", new Class<?>[]{WidgetChatListActions.Model.class}, new Hook(param -> {
                        var _this = (WidgetChatListActions) param.thisObject;
                        var rootView = (NestedScrollView) _this.requireView();
                        var layout = (LinearLayout) rootView.getChildAt(0);
                        var msg = ((WidgetChatListActions.Model) param.args[0]).getMessage();
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

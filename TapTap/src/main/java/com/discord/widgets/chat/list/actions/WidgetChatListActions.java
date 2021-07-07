/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.widgets.chat.list.actions;

import com.discord.api.channel.Channel;
import com.discord.app.AppBottomSheet;
import com.discord.models.message.Message;

public final class WidgetChatListActions extends AppBottomSheet {
    public int getContentViewResId() { return 0; }

    public static final class Model {
        public final Message getMessage() { return getMessage(); }
    }

    public static void access$editMessage(com.discord.widgets.chat.list.actions.WidgetChatListActions widgetChatListActions, Message message) { }
    public static void access$replyMessage(com.discord.widgets.chat.list.actions.WidgetChatListActions widgetChatListActions, Message message, Channel channel) { }
}

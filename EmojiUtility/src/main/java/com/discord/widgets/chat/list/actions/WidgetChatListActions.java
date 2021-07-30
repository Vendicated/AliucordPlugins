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

import com.discord.app.AppBottomSheet;
import com.discord.models.domain.emoji.Emoji;

public class WidgetChatListActions extends AppBottomSheet {
    public static void access$addReaction(WidgetChatListActions w, Emoji emoji) {}

    public int getContentViewResId() { return 0; }
}

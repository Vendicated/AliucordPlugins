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

import android.view.View;

import androidx.annotation.NonNull;

import com.discord.utilities.mg_recycler.MGRecyclerAdapter;
import com.discord.utilities.mg_recycler.MGRecyclerViewHolder;

public final class MoreEmojisViewHolder extends MGRecyclerViewHolder {
    public MoreEmojisViewHolder(@NonNull View itemView, MGRecyclerAdapter mgRecyclerAdapter) { super(itemView, mgRecyclerAdapter); }

    public static WidgetChatListActionsEmojisAdapter access$getAdapter$p(MoreEmojisViewHolder moreEmojisViewHolder) { return access$getAdapter$p(null); }

    public void onConfigure(int i, EmojiItem emojiItem) { }
}

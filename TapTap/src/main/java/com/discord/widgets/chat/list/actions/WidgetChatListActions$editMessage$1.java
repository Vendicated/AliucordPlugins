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
import com.discord.models.message.Message;

import java.util.Map;

import j0.k.b;
import rx.Observable;

public final class WidgetChatListActions$editMessage$1<T, R> implements b<Map<Long, Channel>, Observable<CharSequence>> {
    public WidgetChatListActions$editMessage$1(Message message) { }
    public Observable<CharSequence> call(final Map<Long, Channel> map) { return new Observable<>(); }
}

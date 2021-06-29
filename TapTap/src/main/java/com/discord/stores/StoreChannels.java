/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.discord.stores;

import com.discord.api.channel.Channel;

import java.util.Map;

import rx.Observable;

public class StoreChannels {
    public Channel getChannel(long id) { return new Channel(); }
    public Observable<Map<Long, Channel>> observeGuildAndPrivateChannels() { return new Observable<>(); }
}

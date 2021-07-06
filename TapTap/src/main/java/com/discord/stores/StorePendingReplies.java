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
import com.discord.models.message.Message;

public class StorePendingReplies {
    public void onCreatePendingReply(Channel channel, Message message, boolean shouldMention, boolean showMentionToggle) { }
}

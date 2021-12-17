/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.fixemotes

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreAnalytics
import com.discord.stores.StoreStream

@AliucordPlugin
class FixEmotes : Plugin() {
    val brokenEmoteRegex = "(?<!<a?):[A-Za-z0-9-_]+:".toRegex()
    override fun start(ctx: Context) {
        patcher.before<RestAPIParams.Message>(
            String::class.java,
            String::class.java,
            Long::class.javaObjectType,
            RestAPIParams.Message.Activity::class.java,
            List::class.java,
            RestAPIParams.Message.MessageReference::class.java,
            RestAPIParams.Message.AllowedMentions::class.java,
            String::class.java
        )
        { param ->
            param.args[0] = param.args[0].toString().replace(brokenEmoteRegex) {
                val key = it.value.substring(1, it.value.length - 1)
                val storeEmojis = StoreStream.`access$getCustomEmojis$p`(StoreAnalytics.`access$getStores$p`(StoreStream.getAnalytics()))
                storeEmojis.allGuildEmoji.forEach { (_, g) ->
                    g.values.forEach { e ->
                        if (e.name == key && e.getRegex("").matcher(it.value).matches()) return@replace e.messageContentReplacement
                    }
                }
                it.value
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

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
import com.aliucord.patcher.PreHook
import com.aliucord.patcher.after
import com.discord.restapi.RestAPIParams
import com.discord.widgets.chat.input.emoji.EmojiPickerViewModel

@AliucordPlugin
class FixEmotes : Plugin() {
    private val brokenEmoteRegex = "(?<!<a?):[A-Za-z0-9-_]+:".toRegex()
    override fun start(ctx: Context) {
        var storeState: EmojiPickerViewModel.StoreState.Emoji? = null

        patcher.after<EmojiPickerViewModel>("handleStoreState", EmojiPickerViewModel.StoreState::class.java) { param ->
            (param.args[0] as? EmojiPickerViewModel.StoreState.Emoji)?.let {
                storeState = it
            }
        }

        val ctor = RestAPIParams.Message::class.java.declaredConstructors.firstOrNull {
            !it.isSynthetic
        } ?: throw IllegalStateException("Didn't find RestAPIParams.Message ctor")

        patcher.patch(
            ctor,
            PreHook
            { param ->
                param.args[0] = param.args[0].toString().replace(brokenEmoteRegex) {
                    storeState?.emojiSet?.customEmojis?.forEach { (_, g) ->
                        g.forEach { e ->
                            if (e.getCommand("" /* not used, only there to implement method lmao */) == it.value) {
                                return@replace e.messageContentReplacement
                            }
                        }
                    }
                    it.value
                }
            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

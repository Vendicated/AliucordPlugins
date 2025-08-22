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

    private val brokenEmoteRegex = "(?<!<a?):[A-Za-z0-9_-]+:".toRegex()
    private var emojiMap: Map<String, String> = emptyMap()

    override fun start(ctx: Context) {
  
        patcher.after<EmojiPickerViewModel>(
            "handleStoreState",
            EmojiPickerViewModel.StoreState::class.java
        ) { param ->
            val state = param.args[0] as? EmojiPickerViewModel.StoreState.Emoji ?: return@after
            emojiMap = buildEmojiReplacementMap(state)
        }

   
        val ctor = RestAPIParams.Message::class.java.declaredConstructors.firstOrNull {
            !it.isSynthetic
        } ?: throw IllegalStateException("RestAPIParams.Message constructor not found")

  
        patcher.patch(ctor, PreHook { param ->
            val originalContent = param.args[0].toString()
            val fixedContent = replaceBrokenEmotes(originalContent)
            param.args[0] = fixedContent
        })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

   
    private fun buildEmojiReplacementMap(state: EmojiPickerViewModel.StoreState.Emoji): Map<String, String> {
        val map = mutableMapOf<String, String>()
        state.emojiSet.customEmojis.values.flatten().forEach { emoji ->
            map[emoji.getCommand("")] = emoji.messageContentReplacement
        }
        return map
    }

  
    private fun replaceBrokenEmotes(content: String): String {
        return brokenEmoteRegex.replace(content) { match ->
            emojiMap[match.value] ?: match.value
        }
    }
}

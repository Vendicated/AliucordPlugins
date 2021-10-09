/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/
package dev.vendicated.aliucordplugs.urbandictionary

import android.content.Context
import com.aliucord.Http
import com.aliucord.Http.QueryBuilder
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.embed.MessageEmbed
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

private const val baseUrl = "https://api.urbandictionary.com/v0/define"
private const val thumbsUp = "\uD83D\uDC4D"
private const val thumbsDown = "\uD83D\uDC4E"

@AliucordPlugin
class UrbanDictionary : Plugin() {
    override fun start(context: Context) {
        val arguments = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "search",
                "The word to search for",
                required = true,
                default = true
            ),
            Utils.createCommandOption(
                ApplicationCommandType.BOOLEAN,
                "send",
                "Whether the result should be visible for everyone",
            )
        )

        commands.registerCommand(
            "urban",
            "Get a definition from urbandictionary.com",
            arguments
        ) { ctx ->
            val search = ctx.getRequiredString("search")
            var send = ctx.getBoolOrDefault("send", false)
            var embed: List<MessageEmbed?>? = null
            var result: String
            try {
                val res = Http.simpleJsonGet(QueryBuilder(baseUrl).append("term", search).toString(), ApiResponse::class.java)
                if (res.list.isEmpty()) {
                    result = "No definition found for `$search`"
                    send = false
                } else {
                    val data = res.list[0]
                    val votes = "$thumbsUp ${data.thumbs_up} | $thumbsDown ${data.thumbs_down}"
                    if (send) {
                        result =
"""
**__"${data.word}" on urban dictionary:__**
>>> ${trimLong(data.definition.replace("[", "").replace("]", ""))}

<${data.permalink}>

$votes
"""
                    } else {
                        result = "I found the following:"
                        embed = listOf(
                            MessageEmbedBuilder()
                                .setTitle(data.word)
                                .setUrl(data.permalink)
                                .setDescription(formatUrls(data.definition))
                                .setFooter(votes, null, null)
                                .build()
                        )
                    }
                }
            } catch (t: IOException) {
                result = "Something went wrong: " + t.message
                send = false
            }

            CommandResult(result, embed, send, "UrbanDictionary", "https://www.urbandictionary.com/favicon.ico")
        }
    }

    override fun stop(context: Context) {
        commands.unregisterAll()
    }

    private fun trimLong(str: String): String {
        return if (str.length < 1000) str else str.substring(0, 1000 - 3) + "..."
    }

    private fun formatUrls(raw: String) =
        raw.replace(Regex("\\[.+?\\]")) {
            val value = it.value
            "[$value](https://www.urbandictionary.com/define.php?term=${encodeUri(value.substring(1, value.length - 1))})"
        }

    private fun encodeUri(raw: String): String {
        return try {
            URLEncoder.encode(raw, "UTF-8")
        } catch (ignored: UnsupportedEncodingException) {
            throw AssertionError("UTF-8 is not supported somehow")
        }
    }
}

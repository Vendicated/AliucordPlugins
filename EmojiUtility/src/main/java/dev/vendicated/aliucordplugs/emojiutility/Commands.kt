/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/
package dev.vendicated.aliucordplugs.emojiutility

import com.aliucord.Utils.buildClyde
import com.aliucord.Utils.createCommandOption
import com.aliucord.api.CommandsAPI
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.CommandContext
import com.discord.api.commands.ApplicationCommandType
import com.discord.api.message.MessageFlags
import com.discord.api.message.MessageTypes
import com.discord.models.domain.NonceGenerator
import com.discord.models.message.Message
import com.discord.stores.StoreMessages
import com.discord.stores.StoreStream
import com.discord.utilities.message.LocalMessageCreatorsKt
import com.discord.utilities.time.ClockFactory
import com.discord.utilities.user.UserUtils
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object Commands {
    private val flagsField: Field
    private val typeField: Field
    private val contentField: Field

    @JvmStatic
    fun registerAll(api: CommandsAPI) {
        api.unregisterAll()
        registerDownload(api)
    }

    private fun replyError(msg: String): CommandResult {
        val xEmoji = "❌ "
        return CommandResult(xEmoji + msg, null, false)
    }

    private fun updateMessage(
        ctx: CommandContext,
        ref: AtomicReference<Message?>,
        content: String
    ) {
        val storeMessages = StoreStream.getMessages()
        ref.get()?.let {
            StoreMessages.`access$handleLocalMessageDelete`(
                storeMessages,
                it
            )
        }
        val clock = ClockFactory.get()
        val id = NonceGenerator.computeNonce(clock)
        val msg = LocalMessageCreatorsKt.createLocalApplicationCommandMessage(
            id,
            "download",
            ctx.channelId,
            UserUtils.INSTANCE.synthesizeApiUser(ctx.me),
            buildClyde("Emoji Downloader", null),
            false,
            false,
            id,
            clock
        )
        flagsField[msg] = MessageFlags.EPHEMERAL
        typeField[msg] = MessageTypes.LOCAL
        contentField[msg] = content
        ref.set(msg)
        StoreMessages.`access$handleLocalMessageCreate`(storeMessages, msg)
    }

    private fun registerDownload(api: CommandsAPI) {
        val arguments = listOf(
            createCommandOption(
                ApplicationCommandType.SUBCOMMAND,
                "server",
                "Download all emotes from the current server",
                subCommandOptions = listOf(
                    createCommandOption(
                        ApplicationCommandType.STRING,
                        "guildId",
                        "id of guild to download from",
                    )
                )
            ),
            createCommandOption(
                ApplicationCommandType.SUBCOMMAND,
                "all",
                "Downloads all emotes you have access too",
            ),
            createCommandOption(
                ApplicationCommandType.SUBCOMMAND,
                "reply",
                "Download all emotes from the message this is in reply to",
            ),
            createCommandOption(
                ApplicationCommandType.SUBCOMMAND,
                "provided",
                "Download all emotes passed as argument",
                subCommandOptions = listOf(
                    createCommandOption(
                        ApplicationCommandType.STRING,
                        "emojis",
                        "list of emojis to download",
                        required = true
                    )
                )
            )
        )
        api.registerCommand("download", "Download emojis", arguments) { ctx ->
            val updateMsg = AtomicReference<Message?>()
            val ret: IntArray?
            when {
                ctx.containsArg("server") -> {
                    val guildId =
                        try {
                            (ctx.getSubCommandArgs("server")?.get("guildId") as String?)?.toLong()
                        } catch (th: Throwable) {
                            null
                        }
                            ?: run {
                                if (!ctx.channel.isGuild()) return@registerCommand replyError("Please either specify a valid server id or run this from a server")
                                ctx.channel.guildId
                            }
                    val guild = StoreStream.getGuilds().getGuild(guildId)
                        ?: return@registerCommand replyError("No such guild")
                    updateMessage(
                        ctx,
                        updateMsg,
                        "Downloading ${guild.emojis.size} emojis from ${guild.name}... This might take a while.",
                    )
                    ret = EmojiDownloader.downloadFromGuild(guild)
                }
                ctx.containsArg("all") -> {
                    ret =
                        EmojiDownloader.downloadFromAllGuilds { guildName, progress, amount, stats ->
                            val content =
"""
Downloading emojis from all servers... Go grab a cup of tea or something as this will take a while.

**Current Server**: [$guildName] $progress ($amount emojis)
**Download Stats**: 
```
Success: ${stats[0]}
Already downloaded: ${stats[1]}
Failed: ${stats[2]}
```
"""
                            updateMessage(ctx, updateMsg, content)
                        }
                }
                ctx.containsArg("reply") -> {
                    val msg = ctx.referencedMessage
                        ?: return@registerCommand replyError("You need to reference a message lol")
                    ret = EmojiDownloader.downloadFromString(msg.content ?: return@registerCommand replyError("That message has no content"))
                }
                ctx.containsArg("provided") -> {
                    ret = EmojiDownloader.downloadFromString(
                        ctx.getSubCommandArgs("provided")!!["emojis"] as String
                    )
                }
                else -> {
                    return@registerCommand CommandResult("Please choose a subcommand!", null, false)
                }
            }
            if (ret == null || ret[0] == 0 && ret[1] == 0 && ret[2] == 0) return@registerCommand replyError(
                "No emojis specified"
            )
            updateMessage(
                ctx,
                updateMsg,
                String.format(
                    "Done! Stats: ```\nSuccess: %s\nAlready downloaded: %s\nFailed: %s```",
                    ret[0],
                    ret[1],
                    ret[2]
                )
            )
            null
        }
    }

    init {
        try {
            flagsField = Message::class.java.getDeclaredField("flags").apply {
                isAccessible = true
            }
            typeField = Message::class.java.getDeclaredField("type").apply {
                isAccessible = true
            }
            contentField = Message::class.java.getDeclaredField("content").apply {
                isAccessible = true
            }
        } catch (th: Throwable) {
            throw RuntimeException(th)
        }
    }
}

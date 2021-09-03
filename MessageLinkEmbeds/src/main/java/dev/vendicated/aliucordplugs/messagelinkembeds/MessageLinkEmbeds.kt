/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/
package dev.vendicated.aliucordplugs.messagelinkembeds

import android.content.Context
import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PinePatchFn
import com.aliucord.patcher.PinePrePatchFn
import com.aliucord.utils.ReflectUtils
import com.aliucord.utils.RxUtils.getResultBlocking
import com.aliucord.wrappers.ChannelWrapper.Companion.guildId
import com.aliucord.wrappers.ChannelWrapper.Companion.name
import com.aliucord.wrappers.embeds.ImageWrapper.Companion.height
import com.aliucord.wrappers.embeds.ImageWrapper.Companion.proxyUrl
import com.aliucord.wrappers.embeds.ImageWrapper.Companion.url
import com.aliucord.wrappers.embeds.ImageWrapper.Companion.width
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.color
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.description
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.rawImage
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.rawThumbnail
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.rawVideo
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.url
import com.aliucord.wrappers.embeds.ThumbnailWrapper.Companion.height
import com.aliucord.wrappers.embeds.ThumbnailWrapper.Companion.proxyUrl
import com.aliucord.wrappers.embeds.ThumbnailWrapper.Companion.url
import com.aliucord.wrappers.embeds.ThumbnailWrapper.Companion.width
import com.aliucord.wrappers.embeds.VideoWrapper.Companion.height
import com.aliucord.wrappers.embeds.VideoWrapper.Companion.proxyUrl
import com.aliucord.wrappers.embeds.VideoWrapper.Companion.url
import com.aliucord.wrappers.embeds.VideoWrapper.Companion.width
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.height
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.proxyUrl
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.url
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.width
import com.discord.api.message.embed.EmbedType
import com.discord.api.utcdatetime.UtcDateTime
import com.discord.models.message.Message
import com.discord.models.user.CoreUser
import com.discord.stores.StoreStream
import com.discord.utilities.SnowflakeUtils
import com.discord.utilities.icon.IconUtils
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.uri.UriHandler
import com.discord.utilities.user.UserUtils
import com.discord.utilities.view.text.SimpleDraweeSpanTextView
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.MessageEntry
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern

private val messageLinkPattern =
    Pattern.compile("https?://(?:\\w+\\.)?discord(?:app)?\\.com/channels/(\\d{17,19}|@me)/(\\d{17,19})/(\\d{17,19})")
private val videoLinkPattern =
    Pattern.compile("\\.(mp4|webm|mov)$", Pattern.CASE_INSENSITIVE)

private val logger = Logger("MessageLinkEmbeds")
private val worker = Executors.newSingleThreadExecutor()
private val cache = HashMap<Long, Message>()

private fun addEmbed(
    originalMsg: Message,
    msg: Message,
    url: String,
    messageId: Long,
    channelId: Long
) {
    val eb = MessageEmbedBuilder()
        .setUrl(url)
        .setDescription(msg.content)
        .setTimestamp(UtcDateTime(SnowflakeUtils.toTimestamp(messageId)))

    val author = CoreUser(msg.author).apply {
        val avatarUrl = IconUtils.getForUser(id, avatar, discriminator, true, 256)
        eb.setAuthor(
            username + UserUtils.INSTANCE.getDiscriminatorWithPadding(this),
            avatarUrl,
            avatarUrl
        )
    }

    var setColor = false
    var setThumb = false
    var setImg = false
    var setVideo = false
    var setDesc = msg.content?.isEmpty() == false

    msg.attachments.firstOrNull()?.let { a ->
        if (videoLinkPattern.matcher(a.url).find()) {
            eb.setType(EmbedType.VIDEO)
            eb.setVideo(a.url, a.proxyUrl, a.height, a.width)
            setVideo = true
        } else {
            eb.setImage(a.url, a.proxyUrl, a.height, a.width)
            setImg = true
        }
    }

    msg.embeds.forEach {
        if (!setColor && it.color != null) {
            eb.setColor(it.color)
            setColor = true
        }
        if (!setThumb && it.rawThumbnail != null) {
            it.rawThumbnail?.let { t ->
                eb.setThumbnail(t.url, t.proxyUrl, t.height, t.width)
            }
            setThumb = true
        }
        if (!setImg && it.rawImage != null) {
            it.rawImage?.let { i ->
                eb.setImage(i.url, i.proxyUrl, i.height, i.width)
            }
            setImg = true
        }
        if (!setVideo && it.rawVideo != null) {
            it.rawVideo?.let { v ->
                eb.setVideo(v.url, v.proxyUrl, v.height, v.width)
            }
            setVideo = true
            setImg = true
            eb.setType(EmbedType.VIDEO)
        }
        if (!setDesc && it.description?.isEmpty() == false) {
            eb.setDescription(it.description)
            setDesc = true
        }
    }

    StoreStream.getChannels().getChannel(channelId)?.run {
        val guildStore = StoreStream.getGuilds()
        guildStore.getGuild(guildId)?.let { guild ->
            eb.setFooter(
                "#$name (${guild.name})",
                null,
                null
            )
            if (!setColor) {
                guildStore.getMember(guildId, author.id)?.let { member ->
                    if (member.color != 0) eb.setColor(member.color)
                }
            }
        }
    }

    if (originalMsg.embeds !is ArrayList) {
        try {
            ReflectUtils.setField(originalMsg, "embeds", originalMsg.embeds.toMutableList())
        } catch (th: Throwable) {
            return logger.error(th)
        }
    }

    originalMsg.embeds.add(eb.build())
    StoreStream.getMessages().handleMessageUpdate(originalMsg.synthesizeApiMessage())
}

@AliucordPlugin
class MessageLinkEmbeds : Plugin() {
    override fun start(context: Context) {
        // Patch to jump to message links directly inside Discord instead of launching
        // a new intent like for regular urls. Why is this not a stock feature anyway?????
        patcher.patch(
            UriHandler::class.java.getDeclaredMethod(
                "handle",
                Context::class.java,
                String::class.java,
                Function0::class.java
            ), PinePrePatchFn { cf ->
                val url = cf.args[1] as String
                val matcher = messageLinkPattern.matcher(url)
                if (matcher.find()) {
                    val channelId = matcher.group(2)!!.toLong()
                    val messageId = matcher.group(3)!!.toLong()
                    StoreStream.getMessagesLoader().jumpToMessage(channelId, messageId)
                    cf.result = null
                }
            }
        )

        patcher.patch(
            WidgetChatListAdapterItemMessage::class.java.getDeclaredMethod(
                "processMessageText",
                SimpleDraweeSpanTextView::class.java,
                MessageEntry::class.java
            ), PinePatchFn { callFrame ->
                val msg = (callFrame.args[1] as MessageEntry).message
                if (msg.isLoading) return@PinePatchFn
                val matcher = messageLinkPattern.matcher(msg.content ?: return@PinePatchFn)
                while (matcher.find()) {
                    val url = matcher.group()
                    if (msg.embeds.any { it.url == url }) continue
                    val channelIdStr = matcher.group(2)!!
                    val messageIdStr = matcher.group(3)!!
                    val channelId = channelIdStr.toLong()
                    val messageId = messageIdStr.toLong()

                    val m = cache[messageId] ?: StoreStream.getMessages()
                        .getMessage(channelId, messageId)
                    if (m != null) {
                        addEmbed(msg, m, url, messageId, channelId)
                    } else {
                        val channel =
                            StoreStream.getChannels().getChannel(channelId) ?: return@PinePatchFn
                        val myPerms = StoreStream.getPermissions().permissionsByChannel[channelId]
                        if (!PermissionUtils.INSTANCE.hasAccess(
                                channel,
                                myPerms
                            )
                        ) return@PinePatchFn
                        worker.execute {
                            RestAPI.api.getChannelMessagesAround(channelId, 1, messageId)
                                .getResultBlocking().run {
                                    if (second != null) logger.error(
                                        "Failed to fetch message $url",
                                        second
                                    )
                                    else if (first?.isEmpty() == false) {
                                        Message(first!![0]).apply {
                                            if (id == messageId) {
                                                cache[id] = this
                                                addEmbed(msg, this, url, messageId, channelId)
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        )
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}

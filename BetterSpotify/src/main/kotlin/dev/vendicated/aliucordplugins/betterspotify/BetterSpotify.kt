/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.betterspotify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.*
import androidx.fragment.app.FragmentManager
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.patcher.PreHook
import com.aliucord.utils.RxUtils
import com.aliucord.utils.RxUtils.createActionSubscriber
import com.aliucord.utils.RxUtils.subscribe
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.rawProvider
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.url
import com.aliucord.wrappers.embeds.ProviderWrapper.Companion.name
import com.discord.models.domain.spotify.ModelSpotifyTrack
import com.discord.models.presence.Presence
import com.discord.models.user.User
import com.discord.stores.StoreSpotify
import com.discord.stores.StoreStream
import com.discord.utilities.presence.ActivityUtilsKt
import com.discord.utilities.streams.StreamContext
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemEmbed
import com.discord.widgets.user.presence.ModelRichPresence
import com.discord.widgets.user.presence.ViewHolderMusicRichPresence
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R
import rx.Subscriber
import rx.Subscription
import java.util.concurrent.*

val logger = Logger("BetterSpotify")
val spotifyUrlRe = Regex("https://open.spotify.com/(\\w+)/(\\w+)")

class BetterWebView(ctx: Context) : WebView(ctx) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Scrolling doesn't work without this
        requestDisallowInterceptTouchEvent(true)
        return super.onTouchEvent(event)
    }
}

@AliucordPlugin
@SuppressLint("SetTextI18n")
class BetterSpotify : Plugin() {
    override fun start(_ctx: Context) {
        commands.registerCommand("sendSpotifySong", "Send the currently playing song in chat") {
            val (res, th) = getCurrentTrack()

            when {
                th != null -> CommandsAPI.CommandResult("Something went wrong while retrieving the current song: ${th.message}", null, false)
                res == null -> CommandsAPI.CommandResult("Nothing is playing", null, false)
                else -> CommandsAPI.CommandResult("https://open.spotify.com/track/${res.id}")
            }
        }

        commands.registerCommand("sendSpotifyAlbum", "Send the currently playing album in chat") {
            val (res, th) = getCurrentTrack()

            when {
                th != null -> CommandsAPI.CommandResult("Something went wrong while retrieving the current song: ${th.message}", null, false)
                res == null -> CommandsAPI.CommandResult("Nothing is playing", null, false)
                else -> CommandsAPI.CommandResult("https://open.spotify.com/album/${res.album.id}")
            }
        }

        // Add listen along button
        val btnId = View.generateViewId()
        patcher.patch(
            ViewHolderMusicRichPresence::class.java.getDeclaredMethod(
                "configureUi",
                FragmentManager::class.java,
                StreamContext::class.java,
                Boolean::class.javaPrimitiveType,
                User::class.java,
                Context::class.java,
                ModelRichPresence::class.java,
                Boolean::class.javaPrimitiveType
            ), Hook { param ->
                val holder = param.thisObject as ViewHolderMusicRichPresence
                val primaryBtn = holder.richPresencePrimaryButton
                val layout = primaryBtn.parent as ConstraintLayout
                if (layout.findViewById<View>(btnId) != null) return@Hook
                val ctx = layout.context

                val userId = (param.args[3] as User).id
                MaterialButton(
                    ContextThemeWrapper(ctx, R.h.UserProfile_PresenceSection_MaterialButton), null, 0
                ).run {
                    this.id = btnId
                    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        topToBottom = primaryBtn.id
                    }

                    configureButton(this, userId)
                    (primaryBtn.parent as ViewGroup).addView(this)
                }
            })

        // Detect when the user pauses to leave the listening party
        patcher.patch(
            StoreSpotify::class.java.getDeclaredMethod(
                "access\$setSpotifyState\$p",
                StoreSpotify::class.java,
                StoreSpotify.SpotifyState::class.java
            ), Hook { param ->
                if (subscription == null) return@Hook
                val state = param.args[1] as StoreSpotify.SpotifyState?
                if (state?.playing == false) {
                    RxUtils.timer(10, TimeUnit.SECONDS).subscribe(
                        createActionSubscriber({
                            SpotifyApi.getPlayerInfo { p ->
                                if (!p.is_playing) stopListening()
                            }
                        })
                    )
                }
            })

        // Rich Spotify embeds with play button like on desktop
        val widgetId = View.generateViewId()
        patcher.patch(WidgetChatListAdapterItemEmbed::class.java.getDeclaredMethod("configureUI", WidgetChatListAdapterItemEmbed.Model::class.java), PreHook {
            val model = it.args[0] as WidgetChatListAdapterItemEmbed.Model
            val embed = model.embedEntry.embed
            val holder = it.thisObject as WidgetChatListAdapterItemEmbed
            val layout = holder.itemView as ConstraintLayout

            val existingWebView = layout.findViewById<WebView?>(widgetId)
            if (embed.rawProvider?.name != "Spotify") {
                existingWebView?.visibility = View.GONE
                return@PreHook
            }

            val ctx = layout.context

            val webView = existingWebView ?: BetterWebView(ctx).apply {
                id = widgetId
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true

                val cardView = layout.findViewById<MaterialCardView>(Utils.getResId("chat_list_item_embed_container_card", "id"))
                cardView.addView(this)
            }

            val (_, type, itemId) = spotifyUrlRe.find(embed.url, 0).groupValues
            val url = "https://open.spotify.com/embed/$type/$itemId"

            webView.run {
                visibility = View.VISIBLE
                loadData(
                    """
                                <html>
                                    <body style="margin: 0; padding: 0;">
                                        <iframe
                                            src="$url"
                                            width="100%"
                                            height="${if (type == "track") 80 else 380}"
                                            frameborder="0"
                                            allow="encrypted-media"
                                            allowtransparency
                                            allowfullscreen
                                        />
                                    </body>
                                </html>
                            """,
                    "text/html",
                    "UTF-8"
                )
            }
        })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()

        stopListening()
    }

    private fun getCurrentTrack(): Pair<ModelSpotifyTrack?, Throwable?> {
        val future = CompletableFuture<Any>()

        SpotifyApi.client.spotifyTrack.subscribe(
            createActionSubscriber(
                onNext = {
                    future.complete(it)
                },
                onError = {
                    future.complete(it)
                }
            )
        )

        return future.get().let {
            when (it) {
                is ModelSpotifyTrack -> it to null
                is Throwable -> null to it
                else -> null to null
            }
        }
    }

    private fun configureButton(btn: MaterialButton, userId: Long) {
        with(btn) {
            when (userId) {
                hostId -> {
                    text = "Stop listening along"
                    setOnClickListener {
                        stopListening(shouldPause = true)
                        configureButton(btn, userId)
                    }
                }
                StoreStream.getUsers().me.id -> {
                    text = "Listen along with someone else, dummy..."
                    isEnabled = false
                }
                else -> {
                    text = "Listen Along"
                    setOnClickListener {
                        listenAlong(userId)
                        configureButton(btn, userId)
                    }
                }
            }
        }
    }

    private fun listenAlong(userId: Long) {
        Utils.showToast("Listening along...")
        subscription?.unsubscribe()

        val obs = StoreStream.getPresences().observePresenceForUser(userId)
        hostId = userId
        subscription = obs.subscribe(object : Subscriber<Presence>() {
            override fun onCompleted() {}

            override fun onError(th: Throwable) {
                logger.error("Error while listening along to $userId", th)
            }

            override fun onNext(p: Presence?) {
                if (p == null) return
                p.activities.forEach {
                    if (ActivityUtilsKt.isSpotifyActivity(it)) {
                        val lastSong = currentSong
                        val songId = it.n()
                        currentSong = songId

                        val timestamps = it.o()
                        val start = timestamps.c()
                        endTimestamp = timestamps.b()
                        val offset = (System.currentTimeMillis() - start).toInt()

                        if (songId == lastSong)
                            SpotifyApi.seek(offset)
                        else
                            SpotifyApi.playSong(songId, offset)
                    }
                }
            }
        })
    }

    companion object {
        private var currentSong: String? = null
        private var hostId: Long? = null
        private var subscription: Subscription? = null
        private var endTimestamp: Long? = null

        fun stopListening(shouldPause: Boolean = false, skipToast: Boolean = false) {
            subscription?.let {
                if (!skipToast) Utils.showToast("The listening party has ended!")
                it.unsubscribe()
                if (shouldPause) SpotifyApi.pause()
            }
            subscription = null
            currentSong = null
            hostId = null
        }
    }
}

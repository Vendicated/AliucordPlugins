/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.betterplatformembeds

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.rawProvider
import com.aliucord.wrappers.embeds.MessageEmbedWrapper.Companion.url
import com.aliucord.wrappers.embeds.ProviderWrapper.Companion.name
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemEmbed
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.card.MaterialCardView
import java.util.*

class ScrollableWebView(ctx: Context) : WebView(ctx) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        requestDisallowInterceptTouchEvent(true)
        return super.onTouchEvent(event)
    }
}

@AliucordPlugin
class PlayableEmbeds : Plugin() {
    private val webviewMap = WeakHashMap<WebView, String>()
    private val widgetId = View.generateViewId()
    private val spotifyUrlRe = Regex("https://open\\.spotify\\.com/(\\w+)/(\\w+)")
    private val youtubeUrlRe =
        Regex("(?:https?://)?(?:(?:www|m)\\.)?(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/"+
                "(?:embed/|v/|watch\\?v=|watch\\?.+&v=|shorts/))((\\w|-){11})"+
                "(?:(?:\\?|&)(?:star)?t=(\\d+))?(?:\\S+)?")

    override fun start(_context: Context) {
        patcher.after<WidgetChatListAdapterItemEmbed>("configureUI", WidgetChatListAdapterItemEmbed.Model::class.java) {
            val model = it.args[0] as WidgetChatListAdapterItemEmbed.Model
            val embed = model.embedEntry.embed
            val holder = it.thisObject as WidgetChatListAdapterItemEmbed
            val layout = holder.itemView as ConstraintLayout

            layout.findViewById<WebView?>(widgetId)?.let { v ->
                if (webviewMap[v] == embed.url) return@after
                (v.parent as ViewGroup).removeView(v)
            }
            val url = embed.url ?: return@after;
            when (embed.rawProvider?.name) {
                "YouTube" -> addYoutubeEmbed(layout, url)
                "Spotify" -> addSpotifyEmbed(layout, url)
            }
        }
    }

    private fun addYoutubeEmbed(layout: ViewGroup, url: String) {
        val ctx = layout.context

        val (_, videoId, _, timestamp) = youtubeUrlRe.find(url, 0).groupValues

        val cardView = layout.findViewById<CardView>(Utils.getResId("embed_image_container", "id"))
        val chatListItemEmbedImage = cardView.findViewById<SimpleDraweeView>(Utils.getResId("chat_list_item_embed_image", "id"))
        val playButton = cardView.findViewById<View>(Utils.getResId("chat_list_item_embed_image_icons", "id"))
        playButton.visibility = View.GONE
        chatListItemEmbedImage.visibility = View.GONE

        val webView = ScrollableWebView(ctx).apply {
            id = widgetId
            setBackgroundColor(Color.TRANSPARENT)
            // val maxImgWidth = EmbedResourceUtils.INSTANCE.computeMaximumImageWidthPx(ctx)
            // val (width, height) = EmbedResourceUtils.INSTANCE.calculateScaledSize(1280, 720, maxImgWidth, maxImgWidth, ctx.resources, maxImgWidth / 2)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true

            cardView.addView(this)
        }
        webviewMap[webView] = url

        webView.run {
            loadData(
                """
                <html>
                    <head>
                        <style>
                            body {
                                margin: 0;
                                padding: 0;
                            }
                            .wrapper {
                                position: relative;
                                padding-bottom: 56.25%; /* (100% / 16 * 9), makes the div 16x9 */
                                height: 0;
                                overflow: hidden;
                            }
                            .wrapper iframe {
                                position: absolute;
                                top: 0; 
                                left: 0;
                                width: 100%;
                                height: 100%;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="wrapper">
                            <iframe
                                src="https://www.youtube-nocookie.com/embed/$videoId?start=$timestamp"
                                title="YouTube video player"
                                frameborder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; picture-in-picture"
                                allowfullscreen
                            />
                        </div>
                    </body>
                </html>
                """,
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun addSpotifyEmbed(layout: ViewGroup, url: String) {
        val ctx = layout.context

        val (_, type, itemId) = spotifyUrlRe.find(url, 0).groupValues
        val embedUrl = "https://open.spotify.com/embed/$type/$itemId"

        val webView = (if (type == "track") WebView(ctx) else ScrollableWebView(ctx)).apply {
            id = widgetId
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true

            val cardView = layout.findViewById<MaterialCardView>(Utils.getResId("chat_list_item_embed_container_card", "id"))
            cardView.addView(this)
        }

        webviewMap[webView] = url
        webView.run {
            loadData(
                """
                <html>
                    <body style="margin: 0; padding: 0;">
                        <iframe
                            src="$embedUrl"
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
    }

    @Suppress("Unused")
    private fun addPornHubEmbed() {

    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

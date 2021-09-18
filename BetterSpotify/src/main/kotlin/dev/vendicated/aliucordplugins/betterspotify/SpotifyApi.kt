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

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.utils.ReflectUtils
import com.aliucord.utils.RxUtils
import com.aliucord.utils.RxUtils.createActionSubscriber
import com.aliucord.utils.RxUtils.getResultBlocking
import com.aliucord.utils.RxUtils.subscribe
import com.discord.stores.StoreStream
import com.discord.utilities.platform.Platform
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.spotify.SpotifyApiClient
import dev.vendicated.aliucordplugins.betterspotify.models.PlayerInfo
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val baseUrl = "https://api.spotify.com/v1/me/player"

// The spotify api gives me fucking brain damage i swear to god
// You can either specify album or playlist uris as "context_uri" String or track uris as "uris" array
class SongBody(val uris: List<String>, val position_ms: Int = 0)

object SpotifyApi {
    private val client: SpotifyApiClient by lazy {
        ReflectUtils.getField(StoreStream.getSpotify(), "spotifyApiClient") as SpotifyApiClient
    }

    private var token: String? = null
    private fun getToken(): String? {
        if (token == null) {
            token = RestAPI.AppHeadersProvider.INSTANCE.spotifyToken
                ?: try {
                    val accountId = ReflectUtils.getField(client, "spotifyAccountId")
                    val res = RestAPI.api
                        .getConnectionAccessToken(Platform.SPOTIFY.name.lowercase(), accountId as String)
                        .getResultBlocking()
                    res.second?.let { throw it }
                    res.first!!.accessToken
                } catch (th: Throwable) {
                    null
                }
        }
        return token
    }

    private var didTokenRefresh = false
    private fun request(endpoint: String, method: String = "PUT", data: Any? = null, cb: ((Http.Response) -> Unit)? = null) {
        Utils.threadPool.execute {
            val token = getToken() ?: run {
                Utils.showToast(
                    Utils.appContext,
                    "Failed to get Spotify token from Discord. Make sure your spotify is running."
                )
                return@execute
            }

            try {
                Http.Request("$baseUrl/$endpoint", method)
                    .setHeader("Authorization", "Bearer $token")
                    .use {
                        val res =
                            if (data != null)
                                it.executeWithJson(data)
                            else
                                it
                                    .setHeader("Content-Type", "application/json")
                                    .execute()

                        res.assertOk()
                        cb?.invoke(res)
                    }
            } catch (th: Throwable) {
                if (th is Http.HttpException) {
                    when (th.statusCode) {
                        401 -> {
                            if (!didTokenRefresh) {
                                didTokenRefresh = true
                                SpotifyApiClient.`access$refreshSpotifyToken`(client)
                                this.token = null
                                RxUtils.timer(5, TimeUnit.SECONDS).subscribe(
                                    createActionSubscriber({
                                        request(endpoint, method, data, cb)
                                    })
                                )
                            } else {
                                BetterSpotify.stopListening(skipToast = true)
                                logger.error(Utils.appContext, "Got \"Unauthorized\" Error. Try relinking Spotify")
                            }
                            return@execute
                        }
                        404 -> {
                            BetterSpotify.stopListening(skipToast = true)
                            logger.error(Utils.appContext, "Failed to play. Make sure your Spotify is running", th)
                            return@execute
                        }
                    }
                    logger.error(Utils.appContext, "Failed to play that song :( Check the debug log", th)
                }
            }
        }
    }

    fun getPlayerInfo(cb: (PlayerInfo) -> Unit) {
        request("", "GET", cb = {
            cb.invoke(it.json(PlayerInfo::class.java))
        })
    }

    fun playSong(id: String, position_ms: Int) {
        request("play", "PUT", SongBody(listOf("spotify:track:$id"), position_ms))
    }

    fun pause() {
        request("pause", "PUT")
    }

    fun resume() {
        request("play", "PUT")
    }

    fun seek(position_ms: Int) {
        getPlayerInfo {
            if (abs(it.progress_ms - position_ms) > 5000)
                request("seek?position_ms=$position_ms")
        }
    }
}

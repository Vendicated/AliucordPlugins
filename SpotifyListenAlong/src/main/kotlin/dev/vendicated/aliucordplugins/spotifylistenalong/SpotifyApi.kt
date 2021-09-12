/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.spotifylistenalong

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.utils.ReflectUtils
import com.aliucord.utils.RxUtils.getResultBlocking
import com.discord.stores.StoreStream
import com.discord.utilities.platform.Platform
import com.discord.utilities.rest.RestAPI
import com.discord.utilities.spotify.SpotifyApiClient

const val baseUrl = "https://api.spotify.com/v1/me/player"

// The spotify api gives me fucking brain damage i swear to god
// You can either specify album or playlist uris as "context_uri" String or track uris as "uris" array
class SongBody(val uris: List<String>)

object SpotifyApi {
    private fun request(endpoint: String, method: String, data: Any) {
        Utils.threadPool.execute {
            val token = RestAPI.AppHeadersProvider.INSTANCE.spotifyToken
                ?: try {
                    val client = ReflectUtils.getField(StoreStream.getSpotify(), "spotifyApiClient")
                    val accountId = ReflectUtils.getField(client!!, "spotifyAccountId")
                    val res = RestAPI.api.getConnectionAccessToken(Platform.SPOTIFY.name.lowercase(), accountId as String)
                        .getResultBlocking()
                    res.second?.let { throw it }
                    res.first!!.accessToken
                } catch (th: Throwable) {
                    Utils.showToast(
                        Utils.appContext,
                        "Failed to yoink Spotify token from Discord. Make sure your spotify is running")
                    return@execute
                }

            try {
                Http.Request("$baseUrl/$endpoint", method)
                    .setHeader("Authorization", "Bearer $token")
                    .use {
                        it.executeWithJson(data).run {
                            assertOk()
                        }
                    }
            } catch (th: Throwable) {
                if (th is Http.HttpException && th.statusCode == 401) {
                    val client = ReflectUtils.getField(StoreStream.getSpotify(), "spotifyApiClient")
                    SpotifyApiClient.`access$refreshSpotifyToken`(client as SpotifyApiClient)
                } else
                    logger.error(th)
            }
        }
    }

    fun playSong(id: String) {
        request("play", "PUT", SongBody(listOf("spotify:track:$id")))
    }
}

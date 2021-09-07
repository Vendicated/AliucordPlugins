/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.updater.Updater
import com.discord.stores.StoreStream
import com.discord.utilities.user.UserUtils
import org.json.JSONObject
import java.io.File

class Theme(
    val file: File
) {
    var name: String
    var author: String = "Anonymous"
    var version: String = "1.0.0"
    var license: String? = null
    private var updaterUrl: String? = null

    init {
        name = file.name.removeSuffix(".json")
        val json = json()
        (json.optJSONObject("manifest") ?: json /* legacy format */).run {
            if (has("name")) name = getString("name")
            if (has("author")) author = getString("author")
            if (has("version")) version = getString("version")
            if (has("license")) license = getString("license")
            if (has("updater")) updaterUrl = getString("updater")
        }
    }

    fun json() = JSONObject(file.readText())

    private val prefsKey
        get() = "$name-enabled"

    var isEnabled
        get() = Themer.mSettings.getBool(prefsKey, false)
        set(v) = Themer.mSettings.setBool(prefsKey, v)

    fun convertIfLegacy(): Boolean {
        val json = json()

        val isLegacy = THEME_KEYS.none { json.has(it) }
        if (isLegacy) convertLegacyTheme(this@Theme, json)
        return isLegacy
    }

    fun update() =
        updaterUrl?.let {
            Utils.threadPool.execute {
                try {
                    Http.Request(it).use { req ->
                        val res = req.execute().text()
                        val json = JSONObject(res)
                        if (json.has("version") && Updater.isOutdated("Theme $name", version, json.getString("version"))) {
                            file.writeText(res)
                        }
                    }
                } catch (ex: Throwable) {
                    logger.error("Failed to update theme $name", ex)
                }
            }
        }

    companion object {
    fun create(name: String): Theme {
        val file = File(THEME_DIR, "$name.json")
        val json = JSONObject()
            .put("name", name)
            .put("version", "1.0.0")
            .put("author", StoreStream.getUsers().me.run {
                "$username${UserUtils.INSTANCE.padDiscriminator(discriminator)}"
            })
        file.writeText(json.toString(4))
        return Theme(file)
    }
}
}


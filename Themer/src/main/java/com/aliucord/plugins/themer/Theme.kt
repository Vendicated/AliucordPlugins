package com.aliucord.plugins.themer

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.plugins.Themer
import com.aliucord.plugins.logger
import com.aliucord.updater.Updater
import org.json.JSONObject
import java.io.File

class Theme(
    private val file: File
) {
    var name: String
    var author: String = "Anonymous"
    var version: String = "1.0.0"
    var license: String? = null
    private var updaterUrl: String? = null

    init {
        name = file.name.removeSuffix(".json")
        val json = this.json()
        if (json.has("name")) name = json.getString("name")
        if (json.has("author")) author = json.getString("author")
        if (json.has("version")) version = json.getString("version")
        if (json.has("license")) license = json.getString("license")
        if (json.has("updater")) updaterUrl = json.getString("updater")
    }

    fun json() = JSONObject(file.readText())

    private val prefsKey
        get() = "$name-enabled"

    var isEnabled
        get() = Themer.mSettings.getBool(prefsKey, false)
        set(v) = Themer.mSettings.setBool(prefsKey, v)

    fun enable() {
        ThemeLoader.themes.forEach { it.disable() }
        isEnabled = true
    }

    fun load() = ThemeLoader.loadTheme(this)

    fun disable() {
        isEnabled = false
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
}


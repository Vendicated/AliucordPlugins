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

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.ColorUtils
import com.aliucord.*
import java.io.File

object ThemeLoader {
    val themes = ArrayList<Theme>()

    private fun loadFont(id: Int, url: String) {
        Utils.threadPool.execute {
            try {
                Http.Request(url).execute().run {
                    val file = File(Utils.appActivity.cacheDir, "font-$id.ttf")
                    saveToFile(file)
                    ResourceManager.putFont(id, Typeface.createFromFile(file))
                }
            } catch (ex: Throwable) {
                logger.error("Failed to load font $url", ex)
            }
        }
    }

    private fun loadBackground(url: String) {
        Utils.threadPool.execute {
            try {
                Http.Request(url).execute().stream().use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    ResourceManager.customBg = BitmapDrawable(Utils.appActivity.resources, bitmap)
                    Themer.appContainer?.let {
                        Utils.mainThread.post {
                            Themer.appContainer!!.background = ResourceManager.customBg
                            Themer.appContainer = null
                        }
                    }
                }
            } catch (th: Throwable) {
                logger.error("Failed to load background $url", th)
                Themer.appContainer = null
            }
        }
    }

    fun loadThemes(shouldLoad: Boolean) {
        themes.clear()

        if (!THEME_DIR.exists() && !THEME_DIR.mkdirs()) throw RuntimeException("Failed to create Theme directory ${THEME_DIR.absolutePath}")

        THEME_DIR.listFiles()!!.forEach {
            if (it.name.endsWith(".json")) {
                try {
                    val theme = Theme(it)
                    themes.add(theme)
                    if (shouldLoad && theme.isEnabled) {
                        loadTheme(theme)
                    }
                } catch (th: Throwable) {
                    logger.error("Failed to load theme ${it.name}", th)
                }
            }
        }

        themes.sortBy { it.name }
    }


    private fun loadTheme(theme: Theme): Boolean {
        ResourceManager.bgOpacity = DEFAULT_BACKGROUND_OPACITY

        try {
            if (!theme.convertIfLegacy())
                theme.update()

            val json = theme.json()

            json.optJSONObject("background")?.run {
                keys().forEach {
                    when (it) {
                        "url" -> loadBackground(getString(it))
                        "alpha" -> {
                            val v = getInt(it)
                            if (v !in 0..255) throw IndexOutOfBoundsException("background_transparency must be 0-255, was $v")
                            ResourceManager.bgOpacity = v
                        }
                        else -> logger.warn("[${theme.name}] Unrecognised key: background.$it")
                    }
                }
            }

            json.optJSONObject("fonts")?.run {
                keys().forEach {
                    if (it == "*") loadFont(-1, getString(it))
                    else try {
                        val font = Constants.Fonts::class.java.getField(it)
                        loadFont(font[null] as Int, getString(it))
                    } catch (ex: ReflectiveOperationException) {
                        logger.error("No such font: $it", ex)
                    }
                }
            }

            json.optJSONObject("simple_colors")?.run {
                keys().forEach {
                    val v = getInt(it)
                    when (it) {
                        "accent" -> {
                            ResourceManager.putColors(SIMPLE_ACCENT_NAMES, v)
                            ResourceManager.putAttrs(SIMPLE_ACCENT_ATTRS, v)
                            ResourceManager.putDrawableTint("ic_nitro_rep", v)
                            ResourceManager.putDrawableTint("drawable_voice_indicator_speaking", v)
                        }
                        "background" -> {
                            ResourceManager.putColors(SIMPLE_BG_NAMES, v)
                            ResourceManager.putAttrs(SIMPLE_BG_ATTRS, v)
                        }
                        "background_secondary" -> {
                            ResourceManager.putColors(SIMPLE_BG_SECONDARY_NAMES, v)
                            ResourceManager.putAttrs(SIMPLE_BG_SECONDARY_ATTRS, v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_selected_dark", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_selected_light", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_active_dark", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_active_light", v)
                        }
                        "mention_highlight" -> {
                            ResourceManager.putColor(
                                "status_yellow_500",
                                ColorUtils.setAlphaComponent(v, 0xff)
                            )
                            ResourceManager.putAttr(it, v)
                        }
                        "active_channel" -> {
                            ResourceManager.putDrawableTint("drawable_overlay_channels_active_dark", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_active_light", v)
                            ResourceManager.putColor(it, v)
                        }
                        "statusbar", "input_background" -> ResourceManager.putColor(it, v)
                    }
                }
            }

            json.optJSONObject("colors")?.run {
                if (has("brand_500"))
                    ResourceManager.putDrawableTint(
                        "ic_nitro_rep",
                        getInt("brand_500")
                    )
                keys().forEach {
                    val v = getInt(it)
                    ResourceManager.putColor(it, v)
                    ResourceManager.putAttr(it, v)
                }
            }

            json.optJSONObject("drawable_tints")?.run {
                keys().forEach {
                    ResourceManager.putDrawableTint(it, getInt(it))
                }
            }

        } catch (th: Throwable) {
            logger.error("Failed to load theme ${theme.name}", th)
            return false
        }
        return true
    }
}

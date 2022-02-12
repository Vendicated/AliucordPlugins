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

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.renderscript.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.aliucord.*
import com.aliucord.utils.ReflectUtils
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

object ThemeLoader {
    val themes = ArrayList<Theme>()

    private fun getThemeCacheDir(theme: Theme): File {
        val cacheDir = File(Utils.appContext.cacheDir, "Themer-Cache").also {
            if (!it.exists()) it.mkdir()
        }

        return File(cacheDir, theme.file.name).also {
            if (!it.exists()) it.mkdir()
        }
    }

    private fun getResourceWithCache(theme: Theme, name: String, url: String): File {
        if (url.startsWith("file://"))
            return File(url.removePrefix("file://")).also {
                if (!it.exists()) throw FileNotFoundException(it.absolutePath)
            }

        val cachePrefKey = "${theme.name}-cached-$name"
        val cacheDir = getThemeCacheDir(theme)
        val file = File(cacheDir, name)
        if (!file.exists() || Themer.mSettings.getString(cachePrefKey, "") != url) {
            // Enforce privacy
            verifyUntrustedUrl(url)

            try {
                Utils.threadPool.submit {
                    Http.Request(url).use {
                        it.execute().saveToFile(file)
                        Themer.mSettings.setString(cachePrefKey, url)
                    }
                }
                    // UGLY: Blocks UI thread - necessary to ensure they are loaded in time.
                    // Resources are cached so this point is only reached on first load
                    .get(10, TimeUnit.SECONDS)
            } catch (ex: ExecutionException) {
                throw ex.cause ?: ex
            }
        }
        return file
    }

    private fun loadFont(theme: Theme, id: Int, url: String) {
        try {
            val font = getResourceWithCache(theme, "font_$id", url)
            ResourceManager.putFont(id, Typeface.createFromFile(font))
        } catch (th: Throwable) {
            theme.error("Failed to load font $url with id $id", th)
        }
    }

    private fun loadRaw(theme: Theme, name: String, url: String) {
        try {
            val raw = getResourceWithCache(theme, "raw_$name", url)
            ResourceManager.putRaw(name, raw)
        } catch (th: Throwable) {
            theme.error("Failed to load raw resource $url with name $name", th)
        }
    }

    fun darkenBitmap(bm: Bitmap, alpha: Int) = Canvas(bm).run {
        drawARGB(alpha, 0, 0, 0)
        drawBitmap(bm, Matrix(), Paint())
    }

    // https://stackoverflow.com/a/23119957/11590009
    private fun blurBitmap(bm: Bitmap, blurRadius: Double) {
        if (blurRadius !in 0.0..25.0)
            throw IllegalArgumentException("Blur R.e.us must be 0-25, was $blurRadius")
        val rs = RenderScript.create(Utils.appContext)
        val input = Allocation.createFromBitmap(rs, bm)
        val output = Allocation.createTyped(rs, input.type)

        with(ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))) {
            setRadius(blurRadius.toFloat())
            setInput(input)
            forEach(output)
        }

        output.copyTo(bm)
    }

    private fun loadBackground(theme: Theme, url: String, overlayAlpha: Int, blurRadius: Double) {
        if (overlayAlpha == 0xFF) return
        try {
            val bg = getResourceWithCache(theme, "background", url)
            if (url.endsWith(".gif")) {
                ResourceManager.animatedBgUri = Uri.fromFile(bg)
                ResourceManager.overlayAlpha = overlayAlpha
                return
            }

            val bitmap = BitmapFactory.decodeFile(bg.absolutePath, BitmapFactory.Options().apply {
                inMutable = true
            })
            if (overlayAlpha > 0) darkenBitmap(bitmap, overlayAlpha)
            if (blurRadius > 0) blurBitmap(bitmap, blurRadius)

            ResourceManager.customBg = BitmapDrawable(Utils.appActivity.resources, bitmap)
        } catch (th: Throwable) {
            theme.error("Failed to load background $url", th)
        }
    }


    private fun parseColor(json: JSONObject, key: String): Int {
        val v = json.getString(key)
        return if (v.startsWith("system_")) {
            if (Build.VERSION.SDK_INT < 31)
                throw UnsupportedOperationException("system_ colours are only supported on Android 12.")

            try {
                ContextCompat.getColor(
                    Utils.appContext,
                    ReflectUtils.getField(android.R.color::class.java, null, v) as Int
                )
            } catch (th: Throwable) {
                throw IllegalArgumentException("No such color: $v")
            }
        } else v.toInt()
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
        ResourceManager.overlayAlpha = 0
        try {
            if (!theme.convertIfLegacy())
                theme.update()

            val json = theme.json()

            json.optJSONObject("background")?.run {
                if (has("url")) {
                    val alpha = optInt("overlay_alpha", DEFAULT_OVERLAY_ALPHA)
                    if (alpha !in 0..0xFF)
                        throw IllegalArgumentException("overlay_alpha must be 0-255, was $alpha")
                    loadBackground(theme, getString("url"), alpha, optDouble("blur_radius"))
                }
            }

            json.optJSONObject("fonts")?.run {
                keys().forEach {
                    if (it == "*") loadFont(theme, -1, getString(it))
                    else try {
                        loadFont(
                            theme,
                            ReflectUtils.getField(Constants.Fonts::class.java, null, it) as Int,
                            getString(it)
                        )
                    } catch (ex: ReflectiveOperationException) {
                        theme.error("No such font: $it", ex)
                    }
                }
            }

            json.optJSONObject("raws")?.run {
                keys().forEach {
                    loadRaw(theme, it, getString(it))
                }
            }

            json.optJSONObject("simple_colors")?.run {
                keys().forEach {
                    val v = parseColor(this, it)
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
                            ResourceManager.putDrawableTint("drawable_overlay_channels_selected_dark", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_selected_light", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_active_dark", v)
                            ResourceManager.putDrawableTint("drawable_overlay_channels_active_light", v)
                            ResourceManager.putColor(it, v)
                        }
                        "statusbar", "input_background", "blocked_bg" -> ResourceManager.putColor(it, v)
                    }
                }
            }

            json.optJSONObject("colors")?.run {
                if (has("brand_500"))
                    ResourceManager.putDrawableTint(
                        "ic_nitro_rep",
                        parseColor(this, "brand_500")
                    )
                keys().forEach {
                    val v = parseColor(this, it)
                    ResourceManager.putColor(it, v)
                    ResourceManager.putAttr(it, v)
                }
            }

            json.optJSONObject("drawable_tints")?.run {
                keys().forEach {
                    ResourceManager.putDrawableTint(it, parseColor(this, it))
                }
            }

        } catch (th: Throwable) {
            logger.error("Failed to load theme ${theme.name}", th)
            return false
        }
        return true
    }
}

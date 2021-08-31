package com.aliucord.plugins.themer

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.ColorUtils
import com.aliucord.*
import com.aliucord.plugins.Themer
import com.aliucord.plugins.logger
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

        if (!themeDir.exists() && !themeDir.mkdirs()) throw RuntimeException("Failed to create Theme directory ${themeDir.absolutePath}")

        themeDir.listFiles()!!.forEach {
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

    fun loadTheme(theme: Theme): Boolean {
        ResourceManager.bgOpacity = DEFAULT_BACKGROUND_OPACITY

        try {
            val json = theme.json()

            theme.update()

            if (json.has("simple_accent_color")) {
                val v = json.getInt("simple_accent_color")
                ResourceManager.putColors(ACCENT_NAMES, v)
                ResourceManager.putAttrs(SIMPLE_ACCENT_ATTRS, v)
                ResourceManager.putDrawableTint("ic_nitro_rep", v)
                ResourceManager.putDrawableTint("drawable_voice_indicator_speaking", v)
            }

            if (json.has("simple_bg_color")) {
                val v = json.getInt("simple_bg_color")
                ResourceManager.putColors(BACKGROUND_NAMES, v)
                ResourceManager.putAttrs(SIMPLE_BACKGROUND_ATTRS, v)
            }

            if (json.has("simple_bg_secondary_color")) {
                val v = json.getInt("simple_bg_secondary_color")
                ResourceManager.putColors(BACKGROUND_SECONDARY_NAMES, v)
                ResourceManager.putAttrs(SIMPLE_BACKGROUND_SECONDARY_ATTRS, v)
                ResourceManager.putDrawableTint("drawable_overlay_channels_selected_dark", v)
                ResourceManager.putDrawableTint("drawable_overlay_channels_selected_light", v)
                ResourceManager.putDrawableTint("drawable_overlay_channels_active_dark", v)
                ResourceManager.putDrawableTint("drawable_overlay_channels_active_light", v)
            }

            for (key in json.keys()) {
                val v: Int by lazy {
                    json.getInt(key)
                }

                if (ATTR_MAPPINGS.containsKey(key)) ResourceManager.putAttr(key, v)

                when (key) {
                    "name", "author", "version", "license", "updater", "simple_accent_color", "simple_bg_color", "simple_bg_secondary_color" -> continue
                    "background_url" -> {
                        loadBackground(json.getString(key))
                        continue
                    }
                    "font" -> {
                        loadFont(-1, json.getString(key))
                        continue
                    }
                    "background_transparency" -> {
                        if (v < 0 || v > 255) throw IndexOutOfBoundsException("background_transparency must be 0-255, was $v")
                        ResourceManager.bgOpacity = v
                        continue
                    }
                    "mention_highlight" -> {
                        ResourceManager.putColor(
                            "status_yellow_500",
                            ColorUtils.setAlphaComponent(v, 0xff)
                        )
                        continue
                    }
                    "active_channel_color" -> {
                        ResourceManager.putDrawableTint("drawable_overlay_channels_active_dark", v)
                        ResourceManager.putDrawableTint("drawable_overlay_channels_active_light", v)
                        ResourceManager.putColor(key, v)
                        continue
                    }
                    "statusbar_color", "input_background_color" -> {
                        ResourceManager.putColor(key, v)
                        continue
                    }
                }

                when {
                    key.startsWith("font_") -> {
                        val fontName = key.substring(5)
                        try {
                            val font = Constants.Fonts::class.java.getField(fontName)
                            loadFont(font[null] as Int, json.getString(key))
                        } catch (ex: ReflectiveOperationException) {
                            logger.error("No such font: $fontName", ex)
                        }
                        continue
                    }
                    key.startsWith("color_") -> {
                        if (key == "color_brand_500") ResourceManager.putDrawableTint(
                            "ic_nitro_rep",
                            v
                        )
                        val prefixLength = 6
                        ResourceManager.putColor(key.substring(prefixLength), v)
                        continue
                    }
                    key.startsWith("drawablecolor_") -> {
                        val prefixLength = 14
                        ResourceManager.putDrawableTint(key.substring(prefixLength), v)
                        continue
                    }
                }

                logger.warn("Unrecognised key $key")
            }
        } catch (th: Throwable) {
            logger.error("Failed to load theme ${theme.name}", th)
            return false;
        }
        return true;
    }
}

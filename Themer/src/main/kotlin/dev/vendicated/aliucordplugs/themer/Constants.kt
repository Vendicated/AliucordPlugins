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

import com.aliucord.Constants
import java.io.File
import java.util.regex.Pattern

enum class TransparencyMode(val value: Int) {
    NONE(0),
    CHAT(1),
    CHAT_SETTINGS(2),
    FULL(3);

    companion object {
        fun from(i: Int) = values().first { it.value == i }
    }
}

const val DEFAULT_OVERLAY_ALPHA = 150
val THEME_DIR = File(Constants.BASE_PATH, "themes")

// Credit for these colours to both https://github.com/Aliucord/DiscordThemer
// and https://github.com/GangsterFox/AliuFox-themes/blob/main/ThemerDocu.md

val ALLOWED_RESOURCE_DOMAINS = arrayOf(
    "github.com",
    "raw.githubusercontent.com",
    "gitlab.com",
    "cdn.discordapp.com",
    "media.discordapp.net",
    "i.imgur.com",
    "i.ibb.co", // only for you, FrozenPhoton
)

val ALLOWED_RESOURCE_DOMAINS_PATTERN: Pattern by lazy {
    val domains = ALLOWED_RESOURCE_DOMAINS.joinToString(
        separator = "|",
        transform = { Pattern.quote(it) }
    )
    Pattern.compile("^https://(www)?($domains)/")
}

val THEME_KEYS = arrayOf(
    "manifest",
    "background",
    "fonts",
    "raws",
    "simple_colors",
    "colors",
    "drawable_tints"
)

val SIMPLE_KEYS = arrayOf(
    "accent",
    "background",
    "background_secondary",
    "mention_highlight",
    "active_channel",
    "statusbar",
    "input_background"
)

val SIMPLE_ACCENT_NAMES = arrayOf(
    "link",
    "link_light",
    "brand_new",
    "brand_new_230",
    "brand_new_360", // cursor
    "brand_new_500",
    "brand_new_530",
    "brand_new_560", // reactions
    "brand_new_600",
    // =========== Buttons ============
    "uikit_btn_bg_color_selector_brand",
    "uikit_btn_bg_color_selector_secondary_dark",
    "uikit_btn_compound_color_selector_dark",
    "uikit_btn_compound_color_selector_light",
)

val SIMPLE_BG_NAMES = arrayOf(
    "dark_grey_2",
    "primary_600",
    "primary_660",
    "primary_800",
    "primary_dark_600",
    "primary_dark_630",
    "primary_dark_800"
)

val SIMPLE_BG_SECONDARY_NAMES = arrayOf(
    "primary_500",
    "primary_630",
    "primary_700",
    "primary_dark_660",
    "primary_dark_700",
    "input_background",
    "statusbar",
    "active_channel",
)


val SIMPLE_ACCENT_ATTRS = arrayOf(
    "color_brand",
    "brand_new_500",
    "colorControlBrandForeground",
    "colorControlActivated",
    "colorTextLink",
    "__alpha_10_theme_chat_mention_background",
    "theme_chat_mention_foreground"
)

val SIMPLE_BG_ATTRS = arrayOf(
    "colorSurface",
    "colorBackgroundFloating",
    "colorTabsBackground",
    "theme_chat_spoiler_inapp_bg",
    "primary_600",
    "primary_660",
    "primary_800"
)

val SIMPLE_BG_SECONDARY_ATTRS = arrayOf(
    "colorBackgroundTertiary",
    "colorBackgroundSecondary",
    "primary_700",
    "theme_chat_spoiler_bg"
)

val SIMPLE_SOUND_NAMES = arrayOf("call_calling",
    "call_ringing",
    "deafen",
    "mute",
    "reconnect",
    "stream_ended",
    "stream_started",
    "stream_user_joined",
    "stream_user_left",
    "undeafen",
    "unmute",
    "user_join",
    "user_leave",
    "user_moved"
)

inline fun <reified T> pairOf(v: T) = v to v
inline fun <reified T> pairOf(a: T, b: T) = Pair(a, b)

val ATTR_MAPPINGS = HashMap<String, Array<String>>()

fun initAttrMappings() {
    ATTR_MAPPINGS.clear()
    val map = mapOf(
        pairOf("brand_new") to arrayOf("color_brand"),
        pairOf("brand_new_360", "brand_new_500") to arrayOf("colorControlBrandForeground"),
        pairOf("brand_new_260", "brand_new_530") to arrayOf("theme_chat_mention_foreground"),
        pairOf("brand_new_500") to arrayOf("color_brand_500"),
        pairOf("brand_360", "brand_500") to arrayOf("colorControlActivated"),
        pairOf("brand_500_alpha_20", "brand_new_160") to arrayOf("theme_chat_mention_background"),
        pairOf("link", "link_light") to arrayOf("colorTextLink"),
        pairOf("mention_highlight") to arrayOf("theme_chat_mentioned_me"),

        pairOf("primary_dark_600", "white") to arrayOf("colorBackgroundPrimary"),
        pairOf("primary_dark_800", "white") to arrayOf(
            "colorSurface",
            "colorBackgroundFloating",
            "colorTabsBackground"
        ),
        pairOf("primary_600", "primary_300") to arrayOf("primary_600", "theme_chat_spoiler_inapp_bg"),
        pairOf("primary_630", "white_1") to arrayOf("theme_chat_code"),
        pairOf("primary_660", "white_2") to arrayOf("primary_660", "theme_chat_codeblock_border"),
        pairOf("primary_700", "primary_light_200") to arrayOf(
            "primary_700",
            "colorBackgroundTertiary",
            "colorBackgroundSecondary",
            "theme_chat_spoiler_bg"
        ),
        pairOf("primary_800", "primary_200") to arrayOf("primary_800"),
        pairOf("primary_900", "primary_100") to arrayOf("primary_900"),
    )

    with(ATTR_MAPPINGS) {
        val isLightMode = currentTheme == "light"
        map.forEach { (k, v) ->
            val key = if (isLightMode) k.second else k.first
            if (containsKey(key)) {
                put(key, arrayOf(*get(key)!!, *v))
            } else put(key, v)
        }
    }
}

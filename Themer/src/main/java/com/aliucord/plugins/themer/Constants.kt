package com.aliucord.plugins.themer

import com.aliucord.Constants
import java.io.File

enum class TransparencyMode(val value: Int) {
    NONE(0),
    CHAT(1),
    CHAT_SETTINGS(2),
    FULL(3);

    companion object {
        fun from(i: Int) = values().first { it.value == i}
    }
}

const val DEFAULT_BACKGROUND_OPACITY = 150
val themeDir = File(Constants.BASE_PATH, "themes")

// Credit for these colours to both https://github.com/Aliucord/DiscordThemer
// and https://github.com/GangsterFox/AliuFox-themes/blob/main/ThemerDocu.md

val ACCENT_NAMES = arrayOf(
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

val BACKGROUND_NAMES = arrayOf(
    "dark_grey_2",
    "primary_600",
    "primary_660",
    "primary_800",
    "primary_dark_600",
    "primary_dark_630",
    "primary_dark_800"
)

val BACKGROUND_SECONDARY_NAMES = arrayOf(
    "primary_500",
    "primary_630",
    "primary_700",
    "primary_dark_660",
    "primary_dark_700",
    "input_background_color",
    "statusbar_color",
    "active_channel_color",
)


val SIMPLE_ACCENT_ATTRS = arrayOf(
    "color_brand",
    "colorControlBrandForeground",
    "colorControlActivated",
    "colorTextLink",
    "__alpha_10_theme_chat_mention_background",
    "theme_chat_mention_foreground"
)

val SIMPLE_BACKGROUND_ATTRS = arrayOf(
    "colorSurface",
    "colorBackgroundFloating",
    "colorTabsBackground",
    "theme_chat_spoiler_inapp_bg",
    "primary_600",
    "primary_660",
    "primary_800"
)

val SIMPLE_BACKGROUND_SECONDARY_ATTRS = arrayOf(
    "colorBackgroundTertiary",
    "colorBackgroundSecondary",
    "primary_700",
    "theme_chat_spoiler_bg"
)

val ATTR_MAPPINGS = hashMapOf(
    "color_brand_new" to arrayOf("color_brand"),
    "color_brand_360" to arrayOf("colorControlBrandForeground"),
    "color_primary_dark_800" to arrayOf(
        "colorSurface",
        "colorBackgroundFloating",
        "colorTabsBackground"
    ),
    "color_brand_500" to arrayOf("colorControlActivated"),
    "color_primary_660" to arrayOf("primary_660"),
    "color_primary_600" to arrayOf("primary_600", "theme_chat_spoiler_inapp_bg"),
    "color_primary_700" to arrayOf(
        "primary_700",
        "colorBackgroundTertiary",
        "colorBackgroundSecondary",
        "theme_chat_spoiler_bg"
    ),
    "color_primary_800" to arrayOf("primary_800"),
    "color_primary_900" to arrayOf("primary_900"),
    "color_link" to arrayOf("colorTextLink"),
    "color_brand_500_alpha_20" to arrayOf("theme_chat_mention_background"),
    "color_brand_new_530" to arrayOf("theme_chat_mention_foreground"),
    "mention_highlight" to arrayOf("theme_chat_mentioned_me")
)

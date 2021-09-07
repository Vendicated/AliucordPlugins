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

import org.json.JSONObject

fun convertLegacyTheme(theme: Theme, json: JSONObject) {
    logger.info("Converting legacy theme ${theme.name} to new format...")

    val manifest = JSONObject()
    val background = JSONObject()
    val fonts = JSONObject()
    val simpleColors = JSONObject()
    val colors = JSONObject()
    val drawableTints = JSONObject()

    for (key in json.keys().asSequence().sorted()) {
        when (key) {
            "author", "version", "name", "license", "updater" -> manifest.puts(json, key)

            "background_url" -> background.puts(json, key, "url")
            "background_transparency" -> background.putInt(json, key, "alpha")

            "font" -> fonts.puts(json, key, "*")

            "simple_accent_color" -> simpleColors.putInt(json, key, "accent")
            "simple_bg_color" -> simpleColors.putInt(json, key, "background")
            "simple_bg_secondary_color" -> simpleColors.putInt(json, key, "background_secondary")
            "mention_highlight" -> simpleColors.putInt(json, key)
            "active_channel_color", "statusbar_color", "input_background_color" -> simpleColors.putInt(json, key, key.removeSuffix("_color"))

            else -> {
                when {
                    key.startsWith("color_") -> colors.putInt(json, key, key.substring("color_".length))
                    key.startsWith("drawablecolor_") -> drawableTints.putInt(json, key, key.substring("drawablecolor_".length))
                    key.startsWith("font_") -> fonts.puts(json, key, key.substring("font_".length))
                    else -> logger.warn("[${theme.name}] Unrecognised key $key")
                }
            }
        }
    }

    JSONObject().run {
        put("manifest", manifest)
        put("background", background)
        put("fonts", fonts)
        put("simple_colors", simpleColors)
        put("colors", colors)
        put("drawable_tints", drawableTints)

        theme.file.writeText(toString(4))

        logger.info("Finished converting theme ${theme.name}!")
    }
}

private fun JSONObject.puts(json: JSONObject, key: String, newKey: String = key) = put(newKey, json.getString(key))
private fun JSONObject.putInt(json: JSONObject, key: String, newKey: String = key) = put(newKey, json.getInt(key))

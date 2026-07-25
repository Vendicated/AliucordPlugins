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

import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.ReflectUtils
import dev.vendicated.aliucordplugs.themer.settings.editor.tabs.color.ColorTuple
import org.json.JSONObject

var SettingsAPI.transparencyMode
    get() = TransparencyMode.from(getInt("transparencyMode", TransparencyMode.NONE.value))
    set(v) = setInt("transparencyMode", v.value)

var SettingsAPI.enableFontHook
    get() = getBool("enableFontHook", false)
    set(v) = setBool("enableFontHook", v)

var SettingsAPI.customSounds
    get() = getBool("customSounds", false)
    set(v) = setBool("customSounds", v)

var SettingsAPI.fontHookCausedCrash
    get() = getBool("fontHookCausedCrash", false)
    set(v) = setBool("fontHookCausedCrash", v)

fun JSONObject.parseColor(key: String): Int {
    val v = getString(key)
    return if (v.startsWith("system_")) {
        if (Build.VERSION.SDK_INT < 31)
            throw UnsupportedOperationException("system_ colours are only supported on Android 12.")

        try {
            ContextCompat.getColor(
                Utils.appContext,
                ReflectUtils.getField(android.R.color::class.java, null, v) as Int
            )
        } catch (th: Throwable) {
            throw IllegalArgumentException("No such color: $v", th)
        }
    } else {
        try {
            v.toInt()
        } catch (e: NumberFormatException) {
            try {
                if (v.startsWith("0x", true)) {
                    v.substring(2).toLong(16).toInt()
                } else if (v.startsWith("#")) {
                    Color.parseColor(v)
                } else {
                    v.toLong().toInt()
                }
            } catch (th: Throwable) {
                try {
                    Color.parseColor(v)
                } catch (ignored: Throwable) {
                    throw IllegalArgumentException("No such color or invalid format: $v", th)
                }
            }
        }
    }
}

fun JSONObject.toColorArray() = ArrayList<ColorTuple>().apply {
    keys().forEach {
        try {
            add(ColorTuple(it, parseColor(it)))
        } catch (th: Throwable) {
            logger.error("Failed to parse color for key $it", th)
            add(ColorTuple(it, Color.MAGENTA))
        }
    }
    sortBy { it.name }
}

fun verifyUntrustedUrl(url: String) {
    if (!ALLOWED_RESOURCE_DOMAINS_PATTERN.matcher(url).find())
        throw IllegalArgumentException(
            "URL $url is not allowed. Please use one of: >> ${ALLOWED_RESOURCE_DOMAINS.joinToString()} <<"
        )
}

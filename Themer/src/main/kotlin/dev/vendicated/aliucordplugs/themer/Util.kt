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

import com.aliucord.api.SettingsAPI
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

fun JSONObject.toColorArray() = ArrayList<ColorTuple>().apply {
    keys().forEach {
        add(ColorTuple(it, getInt(it)))
    }
    sortBy { it.name }
}

fun verifyUntrustedUrl(url: String) {
    if (!ALLOWED_RESOURCE_DOMAINS_PATTERN.matcher(url).find())
        throw IllegalArgumentException(
            "URL $url is not allowed. Please use one of: >> ${ALLOWED_RESOURCE_DOMAINS.joinToString()} <<"
        )
}

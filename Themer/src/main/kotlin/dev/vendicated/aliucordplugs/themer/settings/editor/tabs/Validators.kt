/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer.settings.editor.tabs

import android.util.Patterns
import java.util.regex.Pattern

private val versionPattern: Pattern by lazy {
    Pattern.compile("^(\\d{1,2}\\.){2,}\\d{1,2}$")
}

private fun urlValidator(s: String) =
    s.isEmpty() || Patterns.WEB_URL.matcher(s).matches()


object Validators {
    fun manifest(key: String, s: String) = when (key) {
        "version" -> versionPattern.matcher(s).matches()
        "updater" -> urlValidator(s)
        else -> true
    }

    fun background(key: String, s: String) = when (key) {
        "url" -> urlValidator(s)
        "alpha" -> tryOrFalse { s.toInt() in 0..256 }
        else -> throw NotImplementedError(key)
    }
}

val converters = mapOf<String, (s: String) -> Any>(
    "alpha" to { it.toInt() }
)

private fun tryOrFalse(fn: () -> Boolean) = try {
    fn.invoke()
} catch (e: Throwable) {
    false
}

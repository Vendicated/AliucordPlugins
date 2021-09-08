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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import com.aliucord.Constants
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import dev.vendicated.aliucordplugs.themer.ALLOWED_RESOURCE_DOMAINS_PATTERN
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Modifier
import java.util.regex.Pattern

open class FormInputTab(
    header: String,
    private val keys: Array<String>,
    private val validator: (key: String, s: String) -> Boolean,
    private val data: JSONObject
) : Tab(header) {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        val ctx = view.context
        val p = DimenUtils.getDefaultPadding()
        val p2 = p / 2

        keys.forEach {
            TextInput(ctx).run {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(p, p2, p, p2)
                }
                hint = it
                val input = editText!!
                input.setText(data.optString(it))
                input.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(_editable: Editable?) {
                        hint = it
                        val s = input.text.toString()
                        when {
                            s.isEmpty() -> data.remove(it)
                            !validator.invoke(it, s) -> hint = "$it [INVALID]"
                            else -> data.put(it, converters[it]?.invoke(s) ?: s)
                        }
                    }
                })

                linearLayout.addView(this)
            }
        }
    }
}

class ManifestTab(data: JSONObject) : FormInputTab(
    "Manifest", arrayOf(
        "name",
        "author",
        "version",
        "license",
        "updater"
    ), Validators::manifest, data
)

class BackgroundTab(data: JSONObject) : FormInputTab(
    "Background", arrayOf("url", "overlay_alpha", "blur_radius"),
    Validators::background, data
)

class FontTab(data: JSONObject) : FormInputTab(
    "Fonts",
    run {
        try {
            val fonts = Constants.Fonts::class.java.declaredFields
            val names = Array(fonts.size) { "" }
            names[0] = "*"
            fonts.forEachIndexed { i, f ->
                if (Modifier.isPublic(f.modifiers)) {
                    names[i] = f.name
                }
            }
            names
        } catch (th: Throwable) {
            emptyArray()
        }
    }, Validators::fonts, data
)

object Validators {
    private fun tryOrFalse(fn: () -> Boolean) = try {
        fn.invoke()
    } catch (e: Throwable) {
        false
    }

    private val versionPattern: Pattern by lazy {
        Pattern.compile("^(\\d{1,2}\\.){2,}\\d{1,2}$")
    }

    private fun urlValidator(s: String) =
        s.isEmpty() ||
                ALLOWED_RESOURCE_DOMAINS_PATTERN.matcher(s).find() ||
                (s.startsWith("file://") && File(s.removePrefix("file://")).exists())

    fun manifest(key: String, s: String) = when (key) {
        "version" -> versionPattern.matcher(s).matches()
        "updater" -> urlValidator(s)
        else -> true
    }

    fun background(key: String, s: String) = when (key) {
        "url" -> urlValidator(s)
        "overlay_alpha" -> tryOrFalse { s.toInt() in 0..0xFF }
        "blur_radius" -> tryOrFalse { s.toDouble() in 0.0..25.0 }
        else -> throw NotImplementedError(key)
    }

    fun fonts(_key: String, s: String) = urlValidator(s)
}

val converters = mapOf<String, (s: String) -> Any>(
    "overlay_alpha" to { it.toInt() },
    "blur_radius" to { it.toDouble() }
)

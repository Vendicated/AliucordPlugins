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
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import org.json.JSONObject

open class TextInputTab(
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

class ManifestTab(data: JSONObject) : TextInputTab(
    "Manifest", arrayOf(
        "name",
        "author",
        "version",
        "license",
        "updater"
    ), Validators::manifest, data
)

class BackgroundTab(data: JSONObject) : TextInputTab(
    "Background", arrayOf("url", "alpha"),
    Validators::background, data
)

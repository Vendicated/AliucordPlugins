/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.textfilepreview

import android.text.Editable
import android.text.InputType
import android.view.View
import com.aliucord.PluginManager
import com.aliucord.fragments.SettingsPage
import com.aliucord.settings.delegate
import com.aliucord.views.TextInput
import com.discord.utilities.view.text.TextWatcher

class LeSettings() : SettingsPage() {
    private var previewSize: Int by PluginManager.plugins["TextFilePreview"]!!.settings.delegate(300)

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("TextFilePreview")

        TextInput(view.context, "Preview Size (in characters)").run {
            editText.run {
                maxLines = 1
                setText(previewSize.toString())
                inputType = InputType.TYPE_CLASS_NUMBER

                addTextChangedListener(object : TextWatcher() {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        previewSize = try {
                            s.toString().toInt()
                        } catch (th: NumberFormatException) {
                            300
                        }
                    }
                })
            }

            linearLayout.addView(this)
        }
    }
}

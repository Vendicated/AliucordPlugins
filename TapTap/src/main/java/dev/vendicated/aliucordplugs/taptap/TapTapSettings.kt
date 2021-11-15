/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.taptap

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.view.text.TextWatcher
import com.discord.views.CheckedSetting
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class TapTapSettings(private val plugin: TapTap) : SettingsPage() {
    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("TapTap")

        val ctx = view.context

        TextInput(ctx, "Double Tap Window (in ms)").run {
            editText.run {
                maxLines = 1
                setText(plugin.settings.getInt("doubleTapWindow", TapTap.defaultDelay).toString())
                inputType = InputType.TYPE_CLASS_NUMBER

                addTextChangedListener(object : TextWatcher() {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        val i = try {
                            s.toString().toInt()
                        } catch (th: NumberFormatException) {
                            TapTap.defaultDelay
                        }
                        plugin.settings.setInt("doubleTapWindow", i)
                    }
                })
            }

            linearLayout.addView(this)
        }

        addCheckedSetting(
            ctx,
            "Hide Buttons",
            "Hides reply & edit buttons in the message actions menu. The reply button will still be shown on your own messages.",
            "hideButtons"
        )

        addCheckedSetting(
            ctx,
            "Automatically open keyboard",
            "Automatically opens the keyboard when replying to a message",
            "openKeyboard"
        )

        addCheckedSetting(
            ctx,
            "Reply to own messages",
            "Reply to your own messages instead of editing them.",
            "replyToOwn"
        )
    }

    private fun addCheckedSetting(ctx: Context, text: String, subtext: String, key: String) =
        MaterialCardView(ctx).apply {
            radius = DimenUtils.defaultCardRadius.toFloat()
            setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondary))

            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, DimenUtils.defaultPadding, 0, 0)
            }

            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, text, subtext).let {
                it.isChecked = plugin.settings.getBool(key, false)
                it.setOnCheckedListener { checked ->
                    plugin.settings.setBool(key, checked)
                    plugin.togglePatch.run()
                }
                addView(it)
            }

            linearLayout.addView(this)
        }
}

/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer.settings.editor

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.*
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R
import dev.vendicated.aliucordplugs.themer.*
import dev.vendicated.aliucordplugs.themer.settings.ThemerSettings
import dev.vendicated.aliucordplugs.themer.settings.editor.tabs.*
import dev.vendicated.aliucordplugs.themer.settings.editor.tabs.color.ColorTab
import org.json.JSONObject

class ThemeEditor(private val theme: Theme) : SettingsPage() {
    private val json: JSONObject

    init {
        theme.convertIfLegacy()
        json = theme.json().apply {
            THEME_KEYS.forEach {
                if (!has(it)) put(it, JSONObject())
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("Theme Editor")
        setActionBarSubtitle(theme.name)

        val ctx = view.context

        val p = DimenUtils.getDefaultPadding()

        addView(buildEntry(ctx, "Manifest", R.d.ic_audit_logs_24dp) {
            ManifestTab(json.getJSONObject("manifest"))
                .show(parentFragmentManager, "Edit Manifest")
        })

        addView(buildEntry(ctx, "Background", R.d.ic_text_image_24dp) {
            BackgroundTab(json.getJSONObject("background"))
                .show(parentFragmentManager, "Edit background")
        })

        addView(buildEntry(ctx, "Fonts", R.d.ic_edit_24dp) {
            FontTab(json.getJSONObject("fonts"))
                .show(parentFragmentManager, "Edit fonts")
        })

        addView(buildColorEntry(ctx, "Simple Colors", R.d.ic_accessibility_24dp, lazy {
            SIMPLE_KEYS.toList()
        }))
        addView(buildColorEntry(ctx, "Colors", R.d.ic_theme_24dp, lazy {
            R.c::class.java.declaredFields.map { it.name }
        }))
        addView(buildColorEntry(ctx, "Drawable Tints", R.d.ic_emoji_24dp, lazy {
            R.d::class.java.declaredFields.map { it.name }
        }))

        SaveButton(ctx).run {
            setOnClickListener {
                theme.file.writeText(json.toString(4))
                ThemerSettings.promptRestart(view, this@ThemeEditor, "Saved. Restart?")
            }
            linearLayout.addView(this)
        }
    }

    private fun buildColorEntry(ctx: Context, title: String, drawableId: Int, autocompleteOptions: Lazy<List<String>>) =
        buildEntry(ctx, title, drawableId) {
            val key = title.lowercase().replace(" ", "_")
            Utils.openPageWithProxy(it.context, ColorTab(title, json.getJSONObject(key), autocompleteOptions))
        }

    private fun buildEntry(ctx: Context, title: String, drawableId: Int, onClick: View.OnClickListener) =
        TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Icon).apply {
            text = title
            ContextCompat.getDrawable(ctx, drawableId)?.mutate()?.let {
                it.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))
                setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
            }
            setOnClickListener(onClick)
        }
}

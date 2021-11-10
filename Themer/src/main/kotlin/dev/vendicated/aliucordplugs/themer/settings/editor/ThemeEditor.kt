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
import com.aliucord.utils.MDUtils
import com.aliucord.views.*
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R
import dev.vendicated.aliucordplugs.themer.*
import dev.vendicated.aliucordplugs.themer.settings.ThemerSettings
import dev.vendicated.aliucordplugs.themer.settings.editor.tabs.*
import dev.vendicated.aliucordplugs.themer.settings.editor.tabs.color.ColorDialogType
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

        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Addition).run {
            text =
                MDUtils.render("**Allowed hosts for images/fonts/sounds:**\n${ALLOWED_RESOURCE_DOMAINS.joinToString()}")
            addView(this)
        }

        addView(buildEntry(ctx, "Manifest", R.e.ic_audit_logs_24dp) {
            ManifestTab(json.getJSONObject("manifest"))
                .show(parentFragmentManager, "Edit Manifest")
        })

        addView(buildEntry(ctx, "Background", R.e.ic_text_image_24dp) {
            BackgroundTab(json.getJSONObject("background"))
                .show(parentFragmentManager, "Edit background")
        })

        addView(buildEntry(ctx, "Fonts", R.e.ic_edit_24dp) {
            FontTab(json.getJSONObject("fonts"))
                .show(parentFragmentManager, "Edit fonts")
        })

        addView(buildEntry(ctx, "Sounds", R.e.ic_sound_24dp) {
            RawsTab(json.getJSONObject("raws"))
                .show(parentFragmentManager, "Edit sounds")
        })

        addView(buildColorEntry(ctx, ColorDialogType.SIMPLE_COLORS, "Simple Colors", R.e.ic_accessibility_24dp, lazy {
            SIMPLE_KEYS.toList()
        }))
        addView(buildColorEntry(ctx, ColorDialogType.COLORS, "Colors", R.e.ic_theme_24dp, lazy {
            R.c::class.java.declaredFields.map { it.name }
        }))
        addView(buildColorEntry(ctx, ColorDialogType.DRAWABLES, "Drawable Tints", R.e.ic_emoji_24dp, lazy {
            R.e::class.java.declaredFields.map { it.name }
        }))

        SaveButton(ctx).run {
            setOnClickListener {
                theme.file.writeText(json.toString(4))
                ThemerSettings.promptRestart(view, this@ThemeEditor, "Saved. Restart?")
            }
            linearLayout.addView(this)
        }
    }

    private fun buildColorEntry(ctx: Context, type: ColorDialogType, title: String, drawableId: Int, autocompleteOptions: Lazy<List<String>>) =
        buildEntry(ctx, title, drawableId) {
            val key = title.lowercase().replace(" ", "_")
            Utils.openPageWithProxy(it.context, ColorTab(type, title, json.getJSONObject(key), autocompleteOptions))
        }

    private fun buildEntry(ctx: Context, title: String, drawableId: Int, onClick: View.OnClickListener) =
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
            text = title
            ContextCompat.getDrawable(ctx, drawableId)?.mutate()?.let {
                it.setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))
                setCompoundDrawablesRelativeWithIntrinsicBounds(it, null, null, null)
            }
            setOnClickListener(onClick)
        }
}

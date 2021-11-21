/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.aliucord.*
import com.aliucord.fragments.InputDialog
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.ChangelogUtils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.aliucord.views.Divider
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import com.lytefast.flexinput.R
import dev.vendicated.aliucordplugs.themer.*
import kotlin.system.exitProcess

class ThemerSettings : SettingsPage() {
    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        val ctx = view.context

        setActionBarTitle("Themer")

        TextView(ctx, null, 0, R.i.UiKit_TextView).run {
            val content = "Read the changelog!"
            SpannableStringBuilder(content).let {
                it.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val manifest = PluginManager.plugins["Themer"]!!.manifest
                        ChangelogUtils.show(context, manifest.version, manifest.changelogMedia, manifest.changelog)
                    }
                }, content.indexOf("changelog"), content.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = it
            }
            textAlignment = TEXT_ALIGNMENT_CENTER
            DimenUtils.defaultPadding.let {
                setPadding(it, it, it, it)
            }
            movementMethod = LinkMovementMethod.getInstance()
            linearLayout.addView(this)
        }

        Button(ctx).apply {
            text = "Load missing themes"
            setOnClickListener {
                ThemeLoader.loadThemes(false)
                reRender()
            }

            linearLayout.addView(this)
        }

        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Transparency Mode"
            typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold)

            linearLayout.addView(this)
        }

        arrayListOf(
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "None", "No transparency"),
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "Chat", "Chat is transparent"),
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "Chat & Settings", "Chat and Settings page are transparent"),
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "Full", "Everything is transparent. Will only work with themes specifically made for it."),
        ).let { radios ->
            val manager = RadioManager(radios)
            manager.a(radios[Themer.mSettings.transparencyMode.value])
            for (i in 0 until radios.size) {
                val radio = radios[i]
                radio.e {
                    manager.a(radio)
                    Themer.mSettings.transparencyMode = TransparencyMode.from(i)
                    promptRestart(view, this)
                }
                addView(radio)
            }
        }

        addView(Divider(ctx))
        addView(
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, "Enable Custom Fonts", "Temporarily disabled. Refer to the changelog for more info").apply {
                alpha = 0.5f
                isChecked = Themer.mSettings.enableFontHook
                setOnCheckedListener {
                    isChecked = !it
                    // Themer.mSettings.enableFontHook = it
                    // promptRestart(view, this@ThemerSettings)
                }
            }
        )
        addView(Divider(ctx))

        addView(
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.SWITCH, "Enable Custom Sounds", "YOU MUST ENABLE THIS TO USE CUSTOM SOUNDS").apply {
                isChecked = Themer.mSettings.customSounds
                setOnCheckedListener {
                    Themer.mSettings.customSounds = it
                    promptRestart(view, this@ThemerSettings)
                }
            }
        )
        addView(Divider(ctx))

        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).run {
            text = "Themes"
            typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold)
            linearLayout.addView(this)
        }

        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Addition).run {
            text = "Enable multiple themes at your own risk! The result may destroy your eyes."
            linearLayout.addView(this)
        }

        val recycler = RecyclerView(ctx).apply {
            adapter = ThemeAdapter(this@ThemerSettings, ThemeLoader.themes)
            layoutManager = LinearLayoutManager(ctx)

            val decoration = DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL)
            ShapeDrawable(RectShape()).run {
                setTint(Color.TRANSPARENT)
                intrinsicHeight = DimenUtils.defaultPadding
                decoration.setDrawable(this)
            }
            addItemDecoration(decoration)
        }

        Button(ctx).run {
            text = "New Theme"
            DimenUtils.defaultPadding.let {
                setPadding(it, it, it, it)
            }
            setOnClickListener {
                val dialog = InputDialog()
                    .setTitle("New Theme")
                    .setDescription("Please choose a name for your theme")
                    .setPlaceholderText("Name")

                dialog.setOnOkListener {
                    val name = dialog.input
                    if (name.isEmpty()) {
                        Utils.showToast("Cancelled.")
                    } else {
                        try {
                            ThemeLoader.themes.add(0, Theme.create(name))
                            recycler.adapter!!.notifyItemInserted(0)
                            dialog.dismiss()
                        } catch (ex: Throwable) {
                            if (ex is ThemeException) {
                                Utils.showToast(ex.message, true)
                            } else {
                                logger.error(Utils.appContext, "Something went wrong, sorry. Check the debug log for more info", ex)
                            }
                        }
                    }
                }

                dialog.show(parentFragmentManager, "New Theme")
            }
            linearLayout.addView(this)
        }

        addView(recycler)
    }

    companion object {
        fun promptRestart(v: View, fragment: Fragment, msg: String = "Changes detected. Restart?") {
            Snackbar.make(v, msg, LENGTH_INDEFINITE)
                .setAction("Restart") {
                    val ctx = it.context
                    ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.run {
                        fragment.startActivity(Intent.makeRestartActivityTask(component))
                        exitProcess(0)
                    }
                }.show()
        }
    }
}

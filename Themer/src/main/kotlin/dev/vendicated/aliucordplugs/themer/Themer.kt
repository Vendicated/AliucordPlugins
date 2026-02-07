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

import android.content.Context
import android.content.res.Resources
import android.os.Looper
import com.aliucord.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PreHook
import com.aliucord.utils.RxUtils.subscribe
import com.discord.stores.StoreStream
import com.discord.widgets.settings.WidgetSettings
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R
import com.aliucord.Constants
import com.aliucord.Utils
import de.robv.android.xposed.XC_MethodHook
import dev.vendicated.aliucordplugs.themer.settings.ThemerSettings
import rx.Subscription
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

val logger = Logger("Themer")
var currentTheme = ""

@AliucordPlugin
class Themer : Plugin() {
    private var subscription: Subscription? = null

    init {
        settingsTab = SettingsTab(ThemerSettings::class.java)
    }

    override fun start(ctx: Context) {
        currentTheme = StoreStream.getUserSettingsSystem().theme
        subscription = StoreStream.getUserSettingsSystem().observeSettings(false).subscribe {
            if (currentTheme != theme) {
                currentTheme = theme
                initAttrMappings()
            }
        }
        initAttrMappings()
        mSettings = settings
        addPatches(patcher)
        ResourceManager.init(ctx)
        ThemeLoader.loadThemes(true)

        // fixme
        patcher.patch(com.aliucord.Main::class.java.getDeclaredMethod("crashHandler", Thread::class.java, Throwable::class.java), PreHook {
            // Ignore thread exceptions
            if (Looper.getMainLooper().thread != it.args[0]) return@PreHook
            val ex = it.args[1] as? Resources.NotFoundException ?: return@PreHook
            when (ex.stackTrace.firstOrNull()?.methodName) {
                // Crash caused by font hook
                "loadFont", "getFont" -> {
                    settings.enableFontHook = false
                    settings.fontHookCausedCrash = true
                }
            }
        })
        patcher.patch(WidgetSettings::class.java, "onViewBound", arrayOf(View::class.java), object : XC_MethodHook(10000) {
            override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                val layout = ((param.args[0] as ViewGroup).getChildAt(1) as ViewGroup).getChildAt(0) as ViewGroup
                var idx = 0
                while (idx < layout.childCount) {
                    val child = layout.getChildAt(idx)
                    if (child is TextView && child.text.toString().equals("Plugins", ignoreCase = true)) {
                        idx += 1
                        break
                    }
                    idx += 1
                }

                layout.addView(TextView(layout.context, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
                    text = "Themer"
                    typeface = ResourcesCompat.getFont(context, Constants.Fonts.whitney_medium)

                    setCompoundDrawablesRelativeWithIntrinsicBounds(context.getDrawable(R.e.ic_theme_24dp)!!.mutate().apply {
                        setTint(ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal))
                    }, null, null, null);

                    setOnClickListener { Utils.openPageWithProxy(it.context, ThemerSettings()) }
                }, idx)
            }
        })
    }

    override fun stop(context: Context) {
        subscription?.unsubscribe()
        patcher.unpatchAll()
        ResourceManager.clean()
        ThemeLoader.themes.clear()
        Utils.appActivity.recreate()
    }

    companion object {
        lateinit var mSettings: SettingsAPI
    }
}

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
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.utils.RxUtils.subscribe
import com.discord.stores.StoreStream
import dev.vendicated.aliucordplugs.themer.settings.ThemerSettings
import rx.Subscription

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

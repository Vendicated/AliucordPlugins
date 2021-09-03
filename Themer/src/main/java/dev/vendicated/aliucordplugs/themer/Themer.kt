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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import dev.vendicated.aliucordplugs.themer.settings.ThemerSettings

val logger = Logger("Themer")

@AliucordPlugin
class Themer : Plugin() {
    init {
        settingsTab = SettingsTab(ThemerSettings::class.java)
    }

    override fun start(ctx: Context) {
        mSettings = settings
        ResourceManager.init(ctx)
        ThemeLoader.loadThemes(true)
        addPatches(patcher)
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        ResourceManager.clean()
        ThemeLoader.themes.clear()
        Utils.appActivity.recreate()
        appContainer = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Necessary to set the background, manually freed
        var appContainer: View? = null

        lateinit var mSettings: SettingsAPI
    }
}

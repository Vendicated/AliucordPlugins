/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.entities.Plugin.Manifest.Author
import com.aliucord.plugins.themer.*

val logger = Logger("Themer")

class Themer : Plugin() {
    init {
        settingsTab = SettingsTab(ThemerSettings::class.java)
    }

    override fun getManifest() = Manifest().apply {
        authors = arrayOf(Author("Vendicated", 343383572805058560L))
        description = "Apply custom themes to your Discord"
        version = "2.1.0"
        updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json"
        changelog = """
            # Future RoadMap:
            * Theme editor & creator
            * Allow loading multiple themes at once
            * Implement full transparency
            
            # 2.1.0
            * Fix simple_accent_color
            * Revert simple_accent_color theming chat input, timestamps etc
            * simple colours now have less priority, meaning individual colours will always override them
            
            # 2.0.0
            * Rewrite from scratch
            * Theme more things
            * Add Transparency Modes
            * Remove "import theme" button. Please use the download feature (long press message in #themes) or move themes manually to Aliucord/themes
            * Add restart prompt when changing settings
        """.trimIndent()
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
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Necessary to set the background, manually freed
        var appContainer: View? = null

        lateinit var mSettings: SettingsAPI
    }
}

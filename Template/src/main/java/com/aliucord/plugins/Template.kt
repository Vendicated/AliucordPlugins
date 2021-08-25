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

import android.content.Context
import com.aliucord.entities.Plugin
import com.aliucord.entities.Plugin.Manifest.Author

class Template : Plugin() {
    override fun getManifest() = Manifest().apply {
        authors = arrayOf(Author("Vendicated", 343383572805058560L))
        description = "Awesome plugin"
        version = "1.0.0"
        updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json"
        changelog = """
            Epic changes {style marginTop}
            ==============================
            
            * Did some cool things
            * Yeah!!!!
        """.trimIndent()
    }

    override fun start(ctx: Context) {

    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
    }
}
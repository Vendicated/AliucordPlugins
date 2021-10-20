/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.showblockedmessages

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.stores.StoreUserRelationships

fun <K, V> Map<K, V>.mutable() = if (this is HashMap<K, V>) this else HashMap(this)

// Evil Alyxia be like
@AliucordPlugin
class ShowBlockedMessages : Plugin() {
    override fun start(ctx: Context) {
        patcher.patch(StoreUserRelationships::class.java.getDeclaredMethod("getRelationships"), Hook {
            (it.result as Map<Long, Int>).mutable().run {
                forEach { (k, v) ->
                    if (v == 2) put(k, 0)
                }
                it.result = this
            }
        })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

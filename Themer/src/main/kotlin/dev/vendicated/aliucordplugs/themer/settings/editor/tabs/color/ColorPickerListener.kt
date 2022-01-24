/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer.settings.editor.tabs.color

class ColorPickerListener(
    private val adapter: ColorAdapter,
    private val position: Int
) : b.k.a.a.f {
    override fun onColorReset(color: Int) {}

    override fun onColorSelected(_id: Int, color: Int) {
        adapter.filteredItems[position].let {
            it.color = color
            adapter.json.put(it.name, color)
        }
        adapter.notifyItemChanged(position)
    }

    override fun onDialogDismissed(_id: Int) {}
}

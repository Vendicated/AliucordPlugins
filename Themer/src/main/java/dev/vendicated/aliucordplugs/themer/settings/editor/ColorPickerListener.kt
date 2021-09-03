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

class ColorPickerListener(
    private val adapter: ColorAdapter,
    private val position: Int
) : c.k.a.a.f {
    private fun setColor(color: Int) {
        adapter.items[position].color = color
        adapter.notifyItemChanged(position)
    }

    override fun onColorReset(color: Int) = setColor(color)

    override fun onColorSelected(_id: Int, color: Int) = setColor(color)

    override fun onDialogDismissed(_id: Int) {}
}

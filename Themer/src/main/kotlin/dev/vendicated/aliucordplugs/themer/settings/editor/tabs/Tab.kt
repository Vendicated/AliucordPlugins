/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer.settings.editor.tabs

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aliucord.Constants
import com.aliucord.widgets.BottomSheet
import com.lytefast.flexinput.R

abstract class Tab(private val header: String) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        val ctx = view.context
        TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Header).apply {
            text = header
            typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold)
        }.also {
            addView(it)
        }
    }
}

/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.jseval

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aliucord.Constants
import com.lytefast.flexinput.R

@SuppressLint("AppCompatCustomView")
class CodeTextView(ctx : Context) : TextView(ctx, null, 0, R.i.UiKit_TextView) {
    init {
        typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.sourcecodepro_semibold)
        setTextIsSelectable(true)
    }
}

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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import com.aliucord.utils.DimenUtils.dp

@SuppressLint("ViewConstructor")
class TerminalButton(ctx: Context, drawableId: Int, onClick: OnClickListener) : AppCompatImageButton(ctx) {
    init {
        layoutParams = LinearLayout.LayoutParams(30.dp, 30.dp).apply {
            setMargins(6.dp, 0, 6.dp, 0)
        }
        setPadding(6.dp, 6.dp, 6.dp, 6.dp)

        background = ShapeDrawable().apply {
            shape = RectShape()
            paint.color = Color.GRAY
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
        }

        ContextCompat.getDrawable(ctx, drawableId)!!.run {
            mutate()
            setTint(Color.WHITE)
            setImageDrawable(this)
        }
        setOnClickListener(onClick)
    }
}

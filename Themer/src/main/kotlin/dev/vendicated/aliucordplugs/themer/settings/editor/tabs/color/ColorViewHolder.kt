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

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
import android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.utils.DimenUtils
import com.aliucord.views.ToolbarButton
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R

class ColorViewHolder(private val adapter: ColorAdapter, layout: LinearLayout) : RecyclerView.ViewHolder(layout) {
    val textView: TextView
    private val colorCircleView: LinearLayout

    init {
        val ctx = layout.context
        val p = DimenUtils.dpToPx(16)
        val dp48 = DimenUtils.dpToPx(48)

        layout.orientation = LinearLayout.HORIZONTAL
        layout.gravity = Gravity.CENTER_VERTICAL

        ToolbarButton(ctx).run {
            setImageDrawable(ContextCompat.getDrawable(ctx, R.e.ic_x_red_24dp), false)
            layoutParams = LinearLayout.LayoutParams(dp48, dp48).apply {
                marginStart = p
            }
            setOnClickListener {
                adapter.onEntryDeleted(adapterPosition)
            }
            layout.addView(this)
        }

        textView = TextView(ctx, null, 0, R.i.UiKit_Settings_Item).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            setOnClickListener {
                adapter.onEntryClicked(adapterPosition, it.context)
            }
            layout.addView(this)

            TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        }

        colorCircleView = LinearLayout(ctx).apply {
            background = ShapeDrawable(OvalShape())
            layoutParams = LinearLayout.LayoutParams(dp48, dp48).apply {
                marginEnd = p
            }
            setOnClickListener {
                adapter.onEntryClicked(adapterPosition, it.context)
            }
            View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    DimenUtils.dpToPx(1).let {
                        setMargins(it, it, it, it)
                    }
                }
                background = ShapeDrawable(OvalShape())
            }.also {
                addView(it)
            }

            layout.addView(this)
        }
    }

    fun setColor(color: Int) {
        val fullAlphaColor = ColorUtils.setAlphaComponent(color, 0xFF)
        /* Calculate contrast between background and the colour. If the contrast is too low
         * (colours are too similar), calculate the colour's luminance and add a white/black outline
         * accordingly.
         */
        val bgColor = ColorUtils.setAlphaComponent(
            ColorCompat.getThemedColor(colorCircleView.context, R.c.primary_dark_600),
            0xFF
        )

        val contrastColor =
            when {
                ColorUtils.calculateContrast(color, bgColor) > 2f -> fullAlphaColor
                Color.luminance(color) < 0.5f -> Color.WHITE
                else -> Color.BLACK
            }

        colorCircleView.background.colorFilter =
            PorterDuffColorFilter(contrastColor, PorterDuff.Mode.SRC_ATOP)

        colorCircleView.getChildAt(0).background.colorFilter =
            PorterDuffColorFilter(fullAlphaColor, PorterDuff.Mode.SRC_ATOP)

        val alpha = Color.alpha(color)
        if (alpha != 255) {
            colorCircleView.background.alpha = alpha
            colorCircleView.getChildAt(0).background.alpha = alpha
        }
    }
}

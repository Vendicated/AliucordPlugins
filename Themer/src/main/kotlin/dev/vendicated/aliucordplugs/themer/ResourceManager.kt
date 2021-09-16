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

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.graphics.ColorUtils
import com.aliucord.Utils
import com.lytefast.flexinput.R

private var colorToName = HashMap<Int, String>()

private val fonts = HashMap<Int, Typeface>()
private val colorsByName = HashMap<String, Int>()
private val colorsById = HashMap<Int, Int>()
private val drawableTints = HashMap<Int, Int>()
private val attrs = HashMap<Int, Int>()


object ResourceManager {
    var customBg = null as BitmapDrawable?
    var animatedBgUri = null as Uri?
    var overlayAlpha = 0

    fun getColorReplacement(color: Int) = getNameByColor(color)?.let {
        getColorForName(it)
    }

    fun getNameByColor(color: Int) = colorToName[color]
    fun getColorForName(name: String) = colorsByName[name]
    fun getColorForId(id: Int) = colorsById[id]
    fun getDrawableTintForId(id: Int) = drawableTints[id]
    fun getAttrForId(id: Int) = attrs[id]
    fun getFontForId(id: Int) = fonts[id]
    fun getDefaultFont() = getFontForId(-1)

    fun init(ctx: Context) {
        R.c::class.java.declaredFields.forEach {
            val color = ctx.getColor(it.getInt(null))
            if (color != 0) colorToName[color] = it.name
        }
    }

    fun clean() {
        fonts.clear()
        colorsByName.clear()
        colorsById.clear()
        drawableTints.clear()
        attrs.clear()
        customBg = null
        animatedBgUri = null
    }

    internal fun putFont(id: Int, font: Typeface) {
        fonts[id] = font
    }

    internal fun putColor(name: String, color: Int) {
        val id = Utils.getResId(name, "color")
        if (id != 0) {
            colorsById[id] = color
            colorsByName[name] = color
        } else {
            when (name) {
                "statusbar", "input_background", "active_channel" -> colorsByName[name] = color
                else -> logger.warn("Unrecognised colour $name")
            }
        }
    }

    internal fun putColors(names: Array<String>, color: Int) = names.forEach {
        putColor(it, color)
    }

    internal fun putDrawableTint(name: String, color: Int) {
        val id = Utils.getResId(name, "drawable")
        if (id != 0)
            drawableTints[id] = color
        else
            logger.warn("Unrecognised drawable $name")
    }

    internal fun putAttr(name: String?, color: Int) =
        ATTR_MAPPINGS[name]?.forEach {
            setAttr(it, color)
        }

    internal fun putAttrs(attrs: Array<String>, color: Int) {
        attrs.forEach {
            if (it.startsWith("__alpha_10_")) {
                val prefixLen = 11
                setAttr(it.substring(prefixLen), ColorUtils.setAlphaComponent(color, 0x1a))
            } else setAttr(it, color)
        }
    }

    private fun setAttr(attr: String, color: Int) {
        val id = Utils.getResId(attr, "attr")
        if (id == 0) logger.warn("No such attribute: $attr") else attrs[id] = color
    }
}

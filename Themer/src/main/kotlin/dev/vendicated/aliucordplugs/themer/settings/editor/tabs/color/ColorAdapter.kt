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

import android.content.Context
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.discord.utilities.colors.ColorPickerUtils
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import org.json.JSONObject

class ColorAdapter(
    private val fragmentManager: FragmentManager,
    val json: JSONObject,
    val items: ArrayList<ColorTuple>
) : RecyclerView.Adapter<ColorViewHolder>(), Filterable {
    private val layoutHeight = DimenUtils.dpToPx(64)
    var filteredItems = ArrayList(items)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ColorViewHolder(this, com.aliucord.widgets.LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, layoutHeight)
        })

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) =
        filteredItems[position].let { (name, color) ->
            holder.textView.text = name
            holder.setColor(color)
        }

    override fun getItemCount() = filteredItems.size

    fun addItem(item: ColorTuple) {
        items.add(0, item)
        val shouldShow = currentFilter?.let { item.name.contains(it, true) } ?: true
        if (shouldShow) {
            filteredItems.add(0, item)
            notifyItemInserted(0)
        }
    }

    fun onEntryClicked(position: Int, ctx: Context) =
        filteredItems[position].let {
            val colorPicker =
                ColorPickerUtils.INSTANCE.buildColorPickerDialog(
                    ctx,
                    Utils.getResId("color_picker_title", "string"),
                    it.color
                )
            colorPicker.arguments?.putBoolean("alpha", true) // showAlphaSlider
            colorPicker
                .setListener(ColorPickerListener(this, position))
                .show(fragmentManager, "Color Picker")
        }

    fun onEntryDeleted(position: Int) {
        val item = filteredItems[position]
        json.remove(item.name)
        filteredItems.removeAt(position)
        items.remove(item)
        notifyItemRemoved(position)
    }

    override fun getFilter(): Filter = filter

    private var currentFilter: CharSequence? = null
    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
            currentFilter = constraint
            if (constraint == null) {
                count = items.size
                values = items
            } else {
                values = items.filter {
                    it.name.contains(constraint, true)
                }.apply {
                    count = size
                }
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            val data = results.values as ArrayList<*>
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = filteredItems.size

                override fun getNewListSize() = results.count

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    filteredItems[oldItemPosition].equals(data[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true
            }).dispatchUpdatesTo(this@ColorAdapter)
            filteredItems = data as ArrayList<ColorTuple>
        }
    }
}

private fun ColorPickerDialog.setListener(listener: c.k.a.a.f) = apply {
    j = listener
}

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

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.*
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import com.aliucord.widgets.BottomSheet
import com.lytefast.flexinput.R

class NewColorDialog(
    private val options: List<String>,
    private val callback: (s: String) -> Unit
) : BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        val ctx = view.context
        val p = DimenUtils.getDefaultPadding()
        val p2 = p / 2

        val adapter = AutoCompleteAdapter(options) {
            callback.invoke(it)
            dismiss()
        }

        TextInput(ctx).run {
            hint = ctx.getString(R.g.search)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(p, p2, p, p2)
            }
            val input = editText!!
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(_editable: Editable?) {
                    adapter.filter.filter(input.text.toString())
                }
            })
            linearLayout.addView(this)
        }

        RecyclerView(ctx).run {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(ctx)

            val decoration = DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL)
            ShapeDrawable(RectShape()).run {
                setTint(Color.TRANSPARENT)
                intrinsicHeight = DimenUtils.getDefaultPadding()
                decoration.setDrawable(this)
            }
            addItemDecoration(decoration)

            linearLayout.addView(this)
        }
    }
}

private class AutoCompleteAdapter(
    private val items: List<String>,
    private val callback: (s: String) -> Unit
) : RecyclerView.Adapter<AutoCompleteViewHolder>(), Filterable {
    var filteredItems = ArrayList(items)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AutoCompleteViewHolder(
            this,
            TextView(parent.context, null, 0, R.h.UiKit_Settings_Item_Icon)
        )

    override fun onBindViewHolder(holder: AutoCompleteViewHolder, position: Int) {
        (holder.itemView as TextView).text = filteredItems[position]
    }

    override fun getItemCount() = filteredItems.size

    fun onClick(position: Int) = callback.invoke(filteredItems[position])

    override fun getFilter(): Filter = filter

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
            if (constraint == null) {
                count = items.size
                values = items
            } else {
                values = items.filter {
                    it.contains(constraint, true)
                }.apply {
                    count = size
                }
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            val data = results.values as ArrayList<String>
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = filteredItems.size

                override fun getNewListSize() = results.count

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    filteredItems[oldItemPosition].equals(data[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true
            }).dispatchUpdatesTo(this@AutoCompleteAdapter)
            filteredItems = data
        }
    }
}

private class AutoCompleteViewHolder(
    private val adapter: AutoCompleteAdapter,
    itemView: TextView
) : RecyclerView.ViewHolder(itemView) {
    init {
        itemView.setOnClickListener {
            adapter.onClick(adapterPosition)
        }
    }
}

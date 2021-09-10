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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import com.aliucord.widgets.BottomSheet
import com.lytefast.flexinput.R
import dev.vendicated.aliucordplugs.themer.logger

enum class ColorDialogType {
    SIMPLE_COLORS,
    COLORS,
    DRAWABLES
}

class NewColorDialog(
    private val type: ColorDialogType,
    private val options: List<String>,
    private val callback: (s: String) -> Unit,
) : BottomSheet() {
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        val ctx = view.context
        val p = DimenUtils.getDefaultPadding()
        val p2 = p / 2

        val adapter = AutoCompleteAdapter(type, options) {
            callback.invoke(it)
            dismiss()
        }

        if (type != ColorDialogType.SIMPLE_COLORS)
            TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Addition).run {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(p, p, p, 0)
                    textAlignment = TEXT_ALIGNMENT_CENTER
                }
                text = "Tip: Long press an entry to preview it!"
                addView(this)
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
                intrinsicHeight = DimenUtils.getDefaultPadding() / 2
                decoration.setDrawable(this)
            }
            addItemDecoration(decoration)

            linearLayout.addView(this)
        }
    }
}

private class AutoCompleteAdapter(
    private val type: ColorDialogType,
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
        (holder.itemView as TextView).text = items[position]
    }

    override fun getItemCount() = filteredItems.size

    fun onClick(position: Int) = callback.invoke(filteredItems[position])

    fun onLongClick(ctx: Context, position: Int) {
        val name = items[position]

        try {
            var drawable = null as Drawable?
            when (type) {
                ColorDialogType.COLORS -> {
                    val id = Utils.getResId(name, "color")
                    drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(ctx, id))
                        DimenUtils.dpToPx(100).let {
                            setSize(it, it)
                        }
                    }
                }
                ColorDialogType.DRAWABLES -> {
                    val id = Utils.getResId(name, "drawable")
                    drawable = ContextCompat.getDrawable(ctx, id)
                }
                ColorDialogType.SIMPLE_COLORS -> {
                    Utils.showToast(ctx, "Sorry, no preview.")
                }
            }

            Dialog(ctx).run {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                addContentView(
                    ImageView(ctx).apply { setImageDrawable(drawable) },
                    RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                )

                show()
            }

        } catch (th: Throwable) {
            logger.error(ctx, "Failed to open preview", th)
        }
    }

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
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = filteredItems.size

                override fun getNewListSize() = results.count

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    filteredItems[oldItemPosition].equals(data[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true
            })
            filteredItems = data
            diff.dispatchUpdatesTo(this@AutoCompleteAdapter)
        }
    }
}

private class AutoCompleteViewHolder(
    private val adapter: AutoCompleteAdapter,
    itemView: TextView
) : RecyclerView.ViewHolder(itemView) {
    init {
        itemView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        itemView.setOnClickListener {
            adapter.onClick(adapterPosition)
        }
        itemView.setOnLongClickListener {
            adapter.onLongClick(it.context, adapterPosition)
            true
        }
    }
}

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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.*
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.*
import com.aliucord.views.Button
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

        val adapter = AutoCompleteAdapter(ctx, parentFragmentManager, type, options) {
            callback.invoke(it)
            dismiss()
        }

        TextInput(ctx).run {
            hint = ctx.getString(R.g.search)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(p, p2, p, p2)
            }
            editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(e: Editable) {
                    adapter.filter.filter(e.toString())
                }
            })
            linearLayout.addView(this)
        }

        Button(ctx).apply {
            text = "Show Options"
            setOnClickListener {
                adapter.showOptions = !adapter.showOptions
                text = if (adapter.showOptions) "Hide Options" else "Show Options"
                adapter.notifyDataSetChanged()
            }
        }.also {
            addView(it)
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
    ctx: Context,
    private val fm: FragmentManager,
    private val type: ColorDialogType,
    private val originalData: List<String>,
    private val callback: (s: String) -> Unit
) : RecyclerView.Adapter<AutoCompleteViewHolder>(), Filterable {
    var data = ArrayList(originalData)
    var showOptions = false

/*    val infoDrawable = ContextCompat.getDrawable(ctx, R.d.ic_info_24dp)?.apply {
        setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))
    }
    val previewDrawable = ContextCompat.getDrawable(ctx, R.d.ic_spectate)?.apply {
        setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AutoCompleteViewHolder(
            this,
            LinearLayout(parent.context)
        )

    override fun onBindViewHolder(holder: AutoCompleteViewHolder, position: Int) {
        holder.textView.text = data[position]
    }

    override fun getItemCount() = if (showOptions) data.size else 0

    fun onClick(position: Int) = callback.invoke(data[position])

/*    fun onInfoClick(position: Int) {
        val name = data[position]

        ConfirmDialog()
            .setTitle(name)
            .setDescription(info)
            .show(fm, "Documentation Dialog")
    }
 */

    fun onPreviewClick(ctx: Context, position: Int) {
        val name = data[position]

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
                    return Utils.showToast(ctx, "Sorry, no preview.")
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
                values = originalData
                count = originalData.size
            } else {
                originalData.filter { it.contains(constraint, true) }.let {
                    values = it
                    count = it.size
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            val newData = results.values as ArrayList<String>
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = data.size

                override fun getNewListSize() = results.count

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    data[oldItemPosition].equals(newData[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true
            })
            data = newData
            diff.dispatchUpdatesTo(this@AutoCompleteAdapter)
        }
    }
}

private class AutoCompleteViewHolder(
    private val adapter: AutoCompleteAdapter,
    layout: LinearLayout
) : RecyclerView.ViewHolder(layout) {
    val textView: TextView
    // val infoIcon: AppCompatImageButton
    // val viewIcon: AppCompatImageButton

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        textView = TextView(layout.context, null, 0, R.h.UiKit_Settings_Item_Icon).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                weight = 1f
            }
            TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            setOnClickListener {
                adapter.onClick(adapterPosition)
            }
            setOnLongClickListener {
                adapter.onPreviewClick(it.context, adapterPosition)
                true
            }

            layout.addView(this)
        }

/*        val params = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
            val m = DimenUtils.getDefaultPadding() / 2
            leftMargin = m
            rightMargin = m
        }*/

        /*
        infoIcon = ToolbarButton(layout.context).apply {
            setImageDrawable(adapter.infoDrawable, false)
            layoutParams = params
            setOnClickListener {
                adapter.onInfoClick(adapterPosition)
            }

            layout.addView(this)
        }*/

/*        viewIcon = ToolbarButton(layout.context).apply {
            setImageDrawable(adapter.previewDrawable, false)
            layoutParams = params
            setOnClickListener {
                adapter.onPreviewClick(it.context, adapterPosition)
            }

            layout.addView(this)
        }*/
    }
}

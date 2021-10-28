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
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lytefast.flexinput.R
import dev.vendicated.aliucordplugs.themer.toColorArray
import org.json.JSONObject

private val layoutId: Int by lazy {
    Utils.getResId("widget_server_settings_roles", "layout")
}

private val recyclerId: Int by lazy {
    Utils.getResId("server_settings_roles_recycler", "id")
}

private val fabId: Int by lazy {
    Utils.getResId("roles_list_add_role_fab", "id")
}

class ColorTab(
    private val type: ColorDialogType,
    private val header: String,
    private val data: JSONObject,
    private val autoCompleteOptions: Lazy<List<String>>
) : SettingsPage() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        setActionBarTitle(header)
        setActionBarSubtitle("Editor")
        setPadding(0)

        val ctx = view.context
        val p = DimenUtils.defaultPadding
        val p2 = p / 2

        val items = data.toColorArray()
        val adapter = ColorAdapter(parentFragmentManager, data, items)

        TextInput(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(p, p2, p, p2)
            }
            hint = ctx.getString(R.g.search)
            val input = editText!!
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(_editable: Editable?) {
                    adapter.filter.filter(input.text.toString())
                }
            })
        }.also { addView(it) }

        val layout = LayoutInflater.from(ctx)
            .inflate(layoutId, linearLayout, false)
            .also { addView(it) } as ViewGroup


        layout.getChildAt(0).layoutParams.height = 0

        layout.findViewById<RecyclerView>(recyclerId).apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(ctx)
        }

        layout.findViewById<FloatingActionButton>(fabId).run {
            setOnClickListener {
                NewColorDialog(type, autoCompleteOptions.value) {
                    adapter.addItem(ColorTuple(it, Color.BLACK))
                }.show(parentFragmentManager, "New Color")
            }
            show()
        }
    }
}


/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer.settings.editor

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.aliucord.Utils
import com.aliucord.fragments.InputDialog
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.*
import com.lytefast.flexinput.R
import dev.vendicated.aliucordplugs.themer.*
import dev.vendicated.aliucordplugs.themer.settings.ThemerSettings
import org.json.JSONObject
import java.util.regex.Pattern

private val specialKeys = arrayOf(
    "name",
    "author",
    "version",
    "license",
    "updater",
    "background_url",
    "background_transparency",
    "font"
)

val pattern: Pattern by lazy {
    Pattern.compile("^(\\d{1,2}\\.){2,}\\d{1,2}$")
}

private fun versionValidator(s: String) =
    s.isEmpty() || pattern.matcher(s).matches()

private fun transparencyValidator(s: String) =
    s.isEmpty() || try {
        s.toInt() in 0..256
    } catch (e: NumberFormatException) {
        false
    }

private fun urlValidator(s: String) =
    s.isEmpty() || Patterns.WEB_URL.matcher(s).matches()

private val validators = mapOf<String, (s: String) -> Boolean>(
    "version" to ::versionValidator,
    "background_transparency" to ::transparencyValidator,
    "updater" to ::urlValidator,
    "background" to ::urlValidator,
    "font" to ::urlValidator
)

class ThemeEditor(private val theme: Theme) : SettingsPage() {
    private val json = theme.json()
    private val items = ArrayList<ColorTuple>().run {
        json.keys().forEach {
            if (it !in specialKeys) {
                val i = json.optInt(it)
                if (i != 0) add(ColorTuple(it, i))
            }
        }
        sortBy { it.name }

        distinct().toMutableList().apply {
            if (isEmpty()) {
/*                arrayOf(SIMPLE_ACCENT_COLOR, SIMPLE_BG_COLOR, SIMPLE_BG_SECONDARY_COLOR).forEach {
                    add(ColorTuple(it, Color.BLACK))
                }*/
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("Theme Editor")
        setActionBarSubtitle(theme.name)

        val ctx = view.context

        val p = DimenUtils.getDefaultPadding()
        val p2 = p / 2

        specialKeys.forEach {
            TextInput(ctx).run {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(p, p2, p, p2)
                }
                hint = it
                val input = editText!!
                input.setText(json.optString(it))
                input.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(_editable: Editable?) {
                        val s = input.text.toString()
                        if (validators[it]?.invoke(s) == false) {
                            hint = "$it [INVALID]"
                            return
                        }

                        hint = it
                        if (it == "background_transparency")
                            json.put(it, s.toInt())
                        else {
                            json.put(it, s)
                        }
                    }
                })

                linearLayout.addView(this)
            }
        }

        addView(Divider(ctx))

        val recycler = RecyclerView(ctx).apply {
            adapter = ColorAdapter(parentFragmentManager, items)
            layoutManager = LinearLayoutManager(ctx)

            val decoration = DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL)
            ShapeDrawable(RectShape()).run {
                setTint(Color.TRANSPARENT)
                intrinsicHeight = p
                decoration.setDrawable(this)
            }
            addItemDecoration(decoration)
        }

        Button(ctx).run {
            text = "Add Color"
            setPadding(p, p, p, p)
            setOnClickListener {
                val dialog = InputDialog()
                    .setTitle("Add Color")
                    .setDescription("Please choose a name for this color")
                    .setPlaceholderText("Name")

                dialog.setOnOkListener {
                    val name = dialog.input
                    if (name.isEmpty()) {
                        Utils.showToast(ctx, "Cancelled.")
                    } else {
                        items.add(0, ColorTuple(name, Color.BLACK))
                        recycler.adapter!!.notifyItemInserted(0)
                    }
                    dialog.dismiss()
                }
                dialog.show(parentFragmentManager, "New Color")
            }
            linearLayout.addView(this)
        }

        addView(recycler)
        ToolbarButton(ctx).run {
            layoutParams = Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
                marginEnd = p2
            }
            setPadding(p2, p2, p2, p2)
            setImageDrawable(ContextCompat.getDrawable(ctx, R.d.icon_save))
            setOnClickListener {
                JSONObject().let { out ->
                    specialKeys.forEach {
                        val s = json.optString(it)
                        if (s.isNotEmpty()) out.put(it, s)
                    }
                    items.forEach {
                        out.put(it.name, it.color)
                    }

                    theme.file.writeText(out.toString(4))
                    ThemerSettings.promptRestart(view, this@ThemeEditor, "Saved. Restart?")
                }
            }

            addHeaderButton(this)
        }
    }
}

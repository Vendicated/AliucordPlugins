package com.aliucord.plugins.themer.settings.editor

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.utils.DimenUtils
import com.jaredrummler.android.colorpicker.ColorPickerDialog

class ColorAdapter(private val fragmentManager: FragmentManager, val items: MutableList<ColorTuple>) : RecyclerView.Adapter<ColorViewHolder>() {
    private val layoutHeight = DimenUtils.dpToPx(64)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ColorViewHolder(this, com.aliucord.widgets.LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, layoutHeight)
        })

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) =
        items[position].let { (name, color) ->
            holder.textView.text =  name
            holder.colorCircleView.background.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }

    override fun getItemCount() = items.size

    fun onEntryClicked(position: Int) =
        items[position].let {
            colorPickerDialogBuilder()
                .setColor(it.color)
                .create()
                .setListener(ColorPickerListener(this, position))
                .show(fragmentManager, "Color Picker")
        }

    fun onEntryDeleted(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}


private fun colorPickerDialogBuilder() = ColorPickerDialog.k()
private fun ColorPickerDialog.k.create() = this.a()
private fun ColorPickerDialog.k.setColor(color: Int) = apply {
    h = color
}
private fun ColorPickerDialog.setListener(listener: c.k.a.a.f) = apply {
    j = listener
}


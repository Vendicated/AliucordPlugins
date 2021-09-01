package com.aliucord.plugins.themer.settings.editor

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.utils.DimenUtils
import com.aliucord.views.ToolbarButton
import com.lytefast.flexinput.R

class ColorViewHolder(private val adapter: ColorAdapter, layout: LinearLayout) : RecyclerView.ViewHolder(layout) {
    val textView: TextView
    val colorCircleView: View

    init {
        val ctx = layout.context
        val p = DimenUtils.dpToPx(16)
        val p2 = p / 2
        val dp48 = DimenUtils.dpToPx(48)

        layout.orientation = LinearLayout.HORIZONTAL
        layout.gravity = Gravity.CENTER_VERTICAL

        ToolbarButton(ctx).run {
            setPadding(p2, p, p2, p)
            setImageDrawable(ContextCompat.getDrawable(ctx, R.d.ic_x_red_24dp), false)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setOnClickListener {
                adapter.onEntryDeleted(adapterPosition)
            }
            layout.addView(this)
        }

        textView = TextView(ctx, null, 0, R.h.UiKit_TextView).apply {
            setPadding(p2, p, p, p)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1f
            }
            setOnClickListener {
                adapter.onEntryClicked(adapterPosition)
            }
            layout.addView(this)
        }

        colorCircleView = View(ctx).apply {
            setPadding(p, p, p, p)
            background = ShapeDrawable(OvalShape())
            layoutParams = LinearLayout.LayoutParams(dp48, dp48)
            setOnClickListener {
                adapter.onEntryClicked(adapterPosition)
            }
            layout.addView(this)
        }
    }
}

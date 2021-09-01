package com.aliucord.plugins.themer.settings

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Divider
import com.aliucord.views.ToolbarButton
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting
import com.google.android.material.card.MaterialCardView
import com.lytefast.flexinput.R

class ThemeCard(ctx: Context) : MaterialCardView(ctx) {
    val switch: CheckedSetting
    val author: TextView
    val editButton: ToolbarButton
    val uninstallButton: ToolbarButton

    init {
        radius = DimenUtils.getDefaultCardRadius().toFloat()
        setCardBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondary))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        val p = DimenUtils.getDefaultPadding()

        val linearLayout = com.aliucord.widgets.LinearLayout(ctx).apply {
            switch = CheckedSetting(ctx, null).apply {
                removeAllViews()
                f(CheckedSetting.ViewType.SWITCH)
                setSubtext(null)

                k.a().run {
                    textSize = 16.0f
                    typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold)
                    movementMethod = LinkMovementMethod.getInstance()
                }

                k.b().run {
                    setPadding(0, paddingTop, paddingRight, paddingBottom)
                    setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondaryAlt))
                }
            }

            val subLayout = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }

            author = TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Addition).apply {
                setPadding(p, p, p, p)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                    weight = 1f
                }
            }

            editButton = ToolbarButton(ctx).apply {
                setPadding(p, p, p / 2, p)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
                ContextCompat.getDrawable(ctx, R.d.ic_edit_24dp)!!.mutate().let {
                    Utils.tintToTheme(it)
                    setImageDrawable(it, false)
                }
            }

            uninstallButton = ToolbarButton(ctx).apply {
                setPadding(p / 2, p, p, p)
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
                ContextCompat.getDrawable(ctx, R.d.ic_delete_24dp)!!.mutate().let {
                    Utils.tintToTheme(it)
                    setImageDrawable(it, false)
                }
            }

            addView(switch)
            addView(Divider(ctx))
            addView(subLayout.apply {
                addView(author)
                addView(editButton)
                addView(uninstallButton)
            })
        }

        addView(linearLayout)
    }
}

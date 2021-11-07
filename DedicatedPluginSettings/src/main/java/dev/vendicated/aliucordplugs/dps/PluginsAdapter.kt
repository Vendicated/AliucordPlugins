/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.dps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.*
import com.aliucord.entities.Plugin
import com.aliucord.utils.ReflectUtils
import com.discord.utilities.color.ColorCompat
import com.lytefast.flexinput.R
import com.lytefast.flexinput.R.e.*

private operator fun Int.not() =
    ContextCompat.getDrawable(Utils.appContext, this) ?: ContextCompat.getDrawable(Utils.appContext, ic_slash_command_24dp)!!.also {
        Logger("DedicatedPluginSettings")
            .warn(
                "No such drawable: $this (${
                    try {
                        Utils.appActivity.resources.getResourceName(this)
                    } catch (th: Throwable) {
                        "Unknown Resource"
                    }
                })"
            )
    }

private val drawables = hashMapOf(
    "fallback" to !ic_slash_command_24dp,

    // Ven
    "TapTap" to !ic_raised_hand_action_24dp,
    "Themer" to !ic_theme_24dp,
    "Hastebin" to !ic_link_white_24dp,
    "ImageUploader" to !ic_uploads_image_dark,
    "EmojiUtility" to !ic_emoji_24dp,

    // Juby
    "Experiments" to !ic_security_24dp,
    "UserDetails" to !ic_my_account_24dp,
    "PronounDB" to !ic_accessibility_24dp,
    "CustomTimestamps" to !ic_clock_black_24dp,
    "CustomNicknameFormat" to !ic_account_circle_white_24dp,
    "RemoveZoomLimit" to !ic_search_white_24dp,

    // zt
    "AlwaysAnimate" to !ic_play_arrow_24dp,
    "BetterMediaViewer" to !ic_videocam_white_24dp,
    "NoBurnIn" to !ic_x_red_24dp,
    "RotatedChat" to !com.yalantis.ucrop.R.c.ucrop_rotate, // This is from https://github.com/Yalantis/uCrop lmao
    "RoleColorEverywhere" to !ic_theme_24dp,
    "AccountSwitcher" to !ic_account_circle_white_24dp,

    // Xinto
    "LayoutController" to !design_ic_visibility_off,
    "NitroSpoof" to !ic_nitro_rep_24dp,

    // Wing
    "GuildProfiles" to !ic_members_24dp,
    "CustomBadges" to !ic_staff_badge_blurple_24dp,
    "ShowPerms" to !ic_shieldstar_24dp,
    "BetterChannelIcons" to !ic_text_channel_white_24dp,
    "MoreHighlight" to !ic_edit_24dp,

    // Patchouli
    "CopyInsteadOfShareImages" to !ic_link_white_24dp,
    "MediaPickerPatcher" to !ic_gallery_24dp,

    // HalalKing
    "MarqueeEverywhere" to !ic_play_arrow_24dp,
    "PersistSettings" to !ic_settings_24dp,
    "UserBG" to !ic_image_upload,

    // js6pak
    "WhoReacted" to !ic_reaction_24dp,

    // mantika
    "InvisibleMessages" to !design_ic_visibility_off,

    // Tyman
    "Translate" to !ic_locale_24dp,

    // Butterfly
    "BetterStatus" to !ic_phone_24dp,

    // Diamond
    "SplitMessages" to !ic_sort_white_24dp,
    "StreamerMode" to !avd_hide_password,
    "ConfigurableStickerSizes" to !com.yalantis.ucrop.R.c.ucrop_crop
)

class PluginsAdapter : RecyclerView.Adapter<ViewHolder>() {
    val data = PluginManager.plugins.values.filter {
        PluginManager.isPluginEnabled(it.getName()) && it.settingsTab != null
    }.sortedBy {
        it.getName()
    }.toMutableList()

    override fun getItemCount() = data.size.coerceAtLeast(1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            this,
            TextView(parent.context, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
                typeface = ResourcesCompat.getFont(parent.context, Constants.Fonts.whitney_medium)
            }
        )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView as TextView

        if (data.isEmpty()) {
            holder.itemView.text = "Hmm... No Settings"
        } else {
            val p = data[position]
            val name = p.getName()
            holder.itemView.text = name

            val drawable = drawables[name] ?: try {
                ReflectUtils.getField(p, "pluginIcon") as Drawable
            } catch (th: Throwable) {
                null
            } ?: drawables["fallback"]

            holder.itemView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable?.apply {
                mutate()
                setTint(ColorCompat.getThemedColor(holder.itemView.context, R.b.colorInteractiveNormal))
            }, null, null, null)
        }
    }

    fun onEntryClicked(ctx: Context, position: Int) {
        with (data[position]) {
            try {
                val args = settingsTab.args ?: emptyArray()
                when {
                    settingsTab.type == Plugin.SettingsTab.Type.PAGE && settingsTab.page != null -> {
                        ReflectUtils.invokeConstructorWithArgs(settingsTab.page, *args).let {
                            Utils.openPageWithProxy(ctx, it)
                        }
                    }
                    settingsTab.type == Plugin.SettingsTab.Type.BOTTOM_SHEET && settingsTab.bottomSheet != null -> {
                        ReflectUtils.invokeConstructorWithArgs(settingsTab.bottomSheet, *args)
                            .show(
                                DedicatedPluginSettings.widgetSettings?.parentFragmentManager ?: return,
                                getName() + "Settings"
                            )
                    }
                }
            } catch (th: Throwable) {
                PluginManager.logger.error(ctx, "Failed to launch plugin settings", th)
            }
        }
    }
}

class ViewHolder(private val adapter: PluginsAdapter, itemView: TextView) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    init {
        itemView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        adapter.onEntryClicked(v.context, adapterPosition)
    }
}

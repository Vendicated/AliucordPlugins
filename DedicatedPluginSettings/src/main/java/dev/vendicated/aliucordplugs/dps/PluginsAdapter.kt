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
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.*
import com.aliucord.entities.Plugin
import com.aliucord.utils.DimenUtils
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
    "ConfigurableStickerSizes" to !com.yalantis.ucrop.R.c.ucrop_crop,

    // animal will tell you they made a new plugin then show you the most retarded plugin you have ever seen
    "NoKyzas" to !ic_x_red_24dp,
    "PinIcon" to !ic_sidebar_pins_off_light_24dp,
    "MoarConfirm" to !ic_check_white_24dp,
    "GhostMessage" to !ic_chat_message_white_24dp,
    "AutoServerNotifs" to !ic_notifications_white_24dp,
)

val settings = PluginManager.plugins["DedicatedPluginSettings"]!!.settings!!

val hiddenPlugins = HashSet(settings.getString("hiddenPlugins", null)?.split(',') ?: emptyList())

var pluginSort
    get() = settings.getString("pluginOrder", null)?.split(',') ?: emptyList()
    set(plugins) = settings.setString("pluginOrder", plugins.joinToString(","))

class PluginsAdapter(private val touchCallback: DragAndDropHelper) : RecyclerView.Adapter<ViewHolder>() {
    var isEditing = false
    private var originalData = sort(PluginManager.plugins.values.filter {
        PluginManager.isPluginEnabled(it.getName()) && it.settingsTab != null
    }.toMutableList())

    var data = originalData.filter { !hiddenPlugins.contains(it.getName()) } as MutableList<Plugin>

    override fun getItemCount() = data.size.coerceAtLeast(1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            this,
            com.aliucord.widgets.LinearLayout(parent.context)
        )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (data.isEmpty()) {
            holder.textView.text = "Hmm... No Settings"
        } else {
            val p = data[position]
            val name = p.getName()

            holder.textView.text = name

            val drawable = drawables[name] ?: try {
                ReflectUtils.getField(p, "pluginIcon") as Drawable
            } catch (th: Throwable) {
                null
            } ?: drawables["fallback"]

            holder.textView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable?.apply {
                mutate()
                setTint(ColorCompat.getThemedColor(holder.itemView.context, R.b.colorInteractiveNormal))
            }, null, null, null)

            val isHidden = hiddenPlugins.contains(name)
            if (isEditing) {
                holder.itemView.isClickable = false
                holder.visibilitySwitch.visibility = View.VISIBLE
                val visibilityDrawable = if (isHidden) ic_visibility_off_white_a60_24dp else ic_visibility_white_24dp
                holder.visibilitySwitch.setImageDrawable(ContextCompat.getDrawable(holder.visibilitySwitch.context, visibilityDrawable))
            } else {
                holder.itemView.isClickable = true
                holder.visibilitySwitch.visibility = View.GONE
            }
            // RecyclerView Animator uses alpha on the itemView for transitions, so setting alpha on root view doesn't work
            holder.textView.alpha = if (isHidden) 0.5f else 1f
            holder.visibilitySwitch.alpha = if (isHidden) 0.5f else 1f
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

    fun toggleVisibility(position: Int) {
        val name = data[position].getName()
        if (hiddenPlugins.contains(name))
            hiddenPlugins.remove(name)
        else
            hiddenPlugins.add(name)

        notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleCustomize() {
        isEditing = !isEditing
        touchCallback.isEnabled = isEditing
        data = if (!isEditing) {
            settings.setString("hiddenPlugins", hiddenPlugins.joinToString(","))
            pluginSort = originalData.map { it.getName() }
            originalData.filter { !hiddenPlugins.contains(it.getName()) } as ArrayList<Plugin>
        } else {
            originalData
        }
        notifyDataSetChanged()
    }

    fun addPlugin(plugin: Plugin) {
        originalData.add(plugin)
        originalData = sort(originalData)
        if (!hiddenPlugins.contains(plugin.getName())) {
            if (data != originalData) {
                data.add(plugin)
                data = sort(data)
            }
            notifyItemInserted(data.indexOf(plugin))
        }
    }

    fun removePlugin(plugin: Plugin) {
        if (originalData != data) originalData.remove(plugin)
        val idx = data.indexOf(plugin)
        if (idx != -1) {
            data.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reset() {
        hiddenPlugins.clear()
        settings.setString("hiddenPlugins", "")
        pluginSort = emptyList()
        originalData = sort(originalData)
        data = originalData
        notifyDataSetChanged()
    }

    private fun sort(plugins: MutableList<Plugin>): ArrayList<Plugin> {
        val sortedPlugins = ArrayList<Plugin>(plugins.size)
        pluginSort.forEach {
            val idx = plugins.indexOfFirst { p -> p.getName() == it }
            if (idx != -1) {
                sortedPlugins.add(plugins[idx])
                plugins.removeAt(idx)
            }
        }
        plugins.sortBy { it.getName() }
        plugins.forEach { sortedPlugins.add(it) }
        return sortedPlugins
    }
}

class ViewHolder(private val adapter: PluginsAdapter, itemView: LinearLayout) : RecyclerView.ViewHolder(itemView) {
    val textView: TextView
    val visibilitySwitch: ImageView
    init {
        val ctx = itemView.context
        itemView.orientation = LinearLayout.HORIZONTAL
        itemView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        itemView.setOnClickListener(this::onEntryClicked)

        textView = TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                weight = 1f
            }
            typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_medium)
            itemView.addView(this)
        }

        visibilitySwitch = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                gravity = Gravity.END
                marginEnd = DimenUtils.defaultPadding
            }
            setOnClickListener(this@ViewHolder::onVisibilityToggleClicked)
            itemView.addView(this)
        }
    }

    private fun onEntryClicked(v: View) {
        adapter.onEntryClicked(v.context, adapterPosition)
    }

    private fun onVisibilityToggleClicked(v: View) {
        adapter.toggleVisibility(adapterPosition)
    }
}

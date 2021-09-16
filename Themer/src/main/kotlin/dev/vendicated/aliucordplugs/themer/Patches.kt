/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.themer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.*
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentContainerView
import com.aliucord.*
import com.aliucord.api.PatcherAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.PinePatchFn
import com.aliucord.patcher.PinePrePatchFn
import com.aliucord.utils.ReflectUtils
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.filename
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.url
import com.discord.app.*
import com.discord.databinding.WidgetChatListAdapterItemEmbedBinding
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemAttachment
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemEmbed
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.textfield.TextInputLayout
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.lytefast.flexinput.R
import top.canyie.pine.Pine.CallFrame
import top.canyie.pine.callback.MethodHook
import java.io.*
import java.net.URLDecoder
import java.util.regex.Pattern


fun addPatches(patcher: PatcherAPI) {
    patcher.run {
        if (Themer.mSettings.transparencyMode != TransparencyMode.NONE) setBackgrounds()

        patchGetFont()
        patchGetColor()
        patchSetColor()
        patchColorStateLists()
        tintDrawables()
        themeAttributes()
        themeStatusBar()
        themeTextInput()
        addDownloadButton()

        // Set text colour of transparency options in colour picker
        patchColorPicker()
    }
}

val backgroundId = View.generateViewId()
private fun setBackground(view: View, parent: ViewGroup = view as ViewGroup) {
    if (ResourceManager.animatedBgUri != null) {
        if (parent is FragmentContainerView || parent.findViewById<View>(backgroundId) != null) return

        SimpleDraweeView(parent.context).run {
            this.id = backgroundId
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            controller = c.f.g.a.a.b.a().run /*  Fresco.newDraweeControllerBuilder() */ {
                f(ResourceManager.animatedBgUri) // setUri(Uri)
                m = true // mAutoPlayAnimations
                a() // build()
            }
            (parent).addView(this, 0)
        }
    } else if (ResourceManager.customBg != null) {
        view.background = ResourceManager.customBg
    }
}

private fun PatcherAPI.setBackgrounds() {
    val chatId = Utils.getResId("panel_center", "id")

    val transparencyMode = Themer.mSettings.transparencyMode
    if (transparencyMode == TransparencyMode.FULL) {
        val rootId = Utils.getResId("action_bar_root", "id")
        patch(AppFragment::class.java.getDeclaredMethod("onViewBound", View::class.java), PinePatchFn { callFrame: CallFrame ->
            if (ResourceManager.customBg == null && ResourceManager.animatedBgUri == null) return@PinePatchFn
            var view = callFrame.args[0] as View
            val clazz = callFrame.thisObject::class.java
            val cName = clazz.name

            // Discord uses a dialog to show these which makes it so that the chat still shows underneath which makes these
            // unreadable. Thus set their background to the wallpaper
            if (cName == "com.discord.widgets.user.search.WidgetGlobalSearch" ||
                cName == "com.discord.widgets.user.WidgetUserMentions"
            ) {
                setBackground(view)
                // Add darken overlay
                (view as ViewGroup).addView(
                    View(view.context).apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        background = ColorDrawable().apply {
                            color = ColorUtils.setAlphaComponent(Color.BLACK, 100)
                        }
                    }, if (ResourceManager.animatedBgUri != null) 1 else 0
                )

            }

            // Two while loops to first find the root layout or return if it doesn't exist
            // Then go even deeper to find the root root layout
            while (view.id != rootId)
                view = view.parent as View? ?: return@PinePatchFn
            while (true) {
                if (view.parent !is View) break
                view = view.parent as View
            }

            setBackground(view)

            val shouldDarken =
                cName == "com.discord.widgets.debugging.WidgetDebugging" ||
                        cName == "com.discord.widgets.search.WidgetSearch" ||
                        cName.contains("setting", true) ||
                        SettingsPage::class.java.isAssignableFrom(clazz)

            // Add overlay to darken pages, as they would otherwise be too bright
            if (shouldDarken) {
                ((callFrame.args[0] as View).parent as ViewGroup).let {
                    if (it !is FragmentContainerView)
                        it.addView(
                            View(view.context).apply {
                                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                                background = ColorDrawable().apply {
                                    color = ColorUtils.setAlphaComponent(Color.BLACK, 100)
                                }
                            }, 0
                        )
                }
            }
        })

        // Patch for BottomSheet transparency
        patch(AppBottomSheet::class.java.getDeclaredMethod("onViewCreated", View::class.java, Bundle::class.java), PinePatchFn {
            ((it.args[0] as View).parent as View).background = null
        })

        // Fix attachment/embed spoiler overlay being transparent

        val cfgAt =
            WidgetChatListAdapterItemAttachment::class.java.getDeclaredMethod("configureUI", WidgetChatListAdapterItemAttachment.Model::class.java)
        patch(cfgAt, PinePrePatchFn { cf ->
            val binding = WidgetChatListAdapterItemAttachment.`access$getBinding$p`(cf.thisObject as WidgetChatListAdapterItemAttachment)
            binding.root.findViewById<View?>(Utils.getResId("chat_list_item_attachment_spoiler", "id"))?.let {
                val bgColor = (it.background as ColorDrawable?)?.color ?: return@let
                it.setBackgroundColor(ColorUtils.setAlphaComponent(bgColor, 0xFF))
            }
        })

        val cfgEm =
            WidgetChatListAdapterItemEmbed::class.java.getDeclaredMethod("configureUI", WidgetChatListAdapterItemEmbed.Model::class.java)
        patch(cfgEm, PinePrePatchFn { cf ->
            val binding = ReflectUtils.getField(cf.thisObject, "binding") as WidgetChatListAdapterItemEmbedBinding
            binding.root.findViewById<View?>(Utils.getResId("chat_list_item_embed_spoiler", "id"))?.let {
                val bgColor = (it.background as ColorDrawable?)?.color ?: return@let
                it.setBackgroundColor(ColorUtils.setAlphaComponent(bgColor, 0xFF))
            }
        })
    } else {
        val chatBgId = Utils.getResId("widget_home_panel_center_chat", "id")

        patch(AppFragment::class.java.getDeclaredMethod("onViewBound", View::class.java), PinePatchFn { callFrame: CallFrame ->
            if (ResourceManager.customBg == null && ResourceManager.animatedBgUri == null) return@PinePatchFn
            val clazz = callFrame.thisObject.javaClass
            val className = clazz.simpleName
            var view = callFrame.args[0] as View
            if (className == "WidgetChatList") {
                while (view.id != chatId) {
                    view = view.parent as View
                    if (view.id == chatBgId) view.background = null
                }
                setBackground(view)
            } else if (
                transparencyMode == TransparencyMode.CHAT_SETTINGS && (className.lowercase().contains("settings") || SettingsPage::class.java.isAssignableFrom(clazz))
            ) {
                if (ResourceManager.animatedBgUri != null)
                    logger.warn("Animated backgrounds aren't supported on the Chat & Settings setting")
                else
                    setBackground(view)
            }
        })
    }
}

fun fontHook(idx: Int) =
    PinePrePatchFn { callFrame ->
        val font = ResourceManager.getFontForId(callFrame.args[idx] as Int) ?: ResourceManager.getDefaultFont()
        font?.let {
            callFrame.result = it
        }
    }

private fun PatcherAPI.patchGetFont() {
    ResourcesCompat::class.java.declaredMethods.forEach {
        if (it.name == "loadFont") {
            val idIndex = it.parameterTypes.indexOfFirst { p -> p == Int::class.java }
            patch(it, fontHook(idIndex))
        }
    }
}

private fun PatcherAPI.patchGetColor() {
    val patchGetColor: Function1<Int, MethodHook> = { idx ->
        PinePrePatchFn { callFrame: CallFrame ->
            ResourceManager.getColorForId(callFrame.args[idx] as Int)?.let {
                callFrame.result = it
            }
        }
    }

    patch(ColorCompat::class.java.getDeclaredMethod("getThemedColor", Context::class.java, Int::class.javaPrimitiveType), patchGetColor.invoke(1))
    patch(Resources::class.java.getDeclaredMethod("getColor", Int::class.javaPrimitiveType, Resources.Theme::class.java), patchGetColor.invoke(0))
}

private fun PatcherAPI.patchSetColor() {
    patch(
        ColorDrawable::class.java.getDeclaredMethod("setColor", Int::class.javaPrimitiveType),
        PinePrePatchFn { callFrame ->
            ResourceManager.getColorReplacement(callFrame.args[0] as Int)?.let {
                callFrame.args[0] = it
            }
        }
    )
}

private fun PatcherAPI.patchColorStateLists() {
    // Figure out better way to do this
    // This is stupid, because it matches the wrong name because we dont work with ids here but rather the colour value so its
    // impossible to consistently resolve the correct name
    patch(
        ColorStateList::class.java.getDeclaredMethod("getColorForState", IntArray::class.java, Int::class.javaPrimitiveType),
        PinePatchFn { callFrame: CallFrame ->
            ResourceManager.getColorReplacement(callFrame.result as Int)?.let {
                callFrame.result = it
            }
        })
}

private fun PatcherAPI.tintDrawables() {
    patch(
        Resources::class.java.getDeclaredMethod(
            "getDrawableForDensity",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Resources.Theme::class.java
        ), PinePatchFn { callFrame: CallFrame ->
            ResourceManager.getDrawableTintForId(callFrame.args[0] as Int)?.let {
                (callFrame.result as Drawable?)?.setTint(it)
            }
        })
}

private fun PatcherAPI.themeAttributes() {
    patch(
        Resources.Theme::class.java.getDeclaredMethod(
            "resolveAttribute",
            Int::class.javaPrimitiveType,
            TypedValue::class.java,
            Boolean::class.javaPrimitiveType
        ), PinePatchFn { callFrame: CallFrame ->
            ResourceManager.getAttrForId(callFrame.args[0] as Int)?.let {
                (callFrame.args[1] as TypedValue).data = it
            }
        })
}

private fun PatcherAPI.themeStatusBar() {
    patch(
        ColorCompat::class.java.getDeclaredMethod(
            "setStatusBarColor",
            Window::class.java,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        ), PinePrePatchFn { callFrame ->
            ResourceManager.getColorForName("statusbar")?.let {
                callFrame.args[1] = it
            }
        })
}

private fun PatcherAPI.themeTextInput() {
    patch(TextInputLayout::class.java.getDeclaredMethod("calculateBoxBackgroundColor"), PinePrePatchFn { callFrame: CallFrame ->
        ResourceManager.getColorForName("input_background")?.let {
            callFrame.result = it
        }
    })
}

const val THEMES_CHANNEL_ID = 824357609778708580L

@SuppressLint("SetTextI18n")
private fun PatcherAPI.addDownloadButton() {
    val emojiTrayId = Utils.getResId("dialog_chat_actions_add_reaction_emojis_list", "id")
    val id = View.generateViewId()
    val badUrlMatcher = Pattern.compile("http[^\\s]+\\.json")

    patch(
        WidgetChatListActions::class.java,
        "configureUI",
        arrayOf<Class<*>>(WidgetChatListActions.Model::class.java),
        PinePatchFn { callFrame: CallFrame ->
            val layout =
                ((callFrame.thisObject as WidgetChatListActions).requireView() as ViewGroup).getChildAt(0) as ViewGroup?
                    ?: return@PinePatchFn

            if (layout.findViewById<View>(id) != null) return@PinePatchFn

            val idx = if (emojiTrayId == 0) 1 else layout.indexOfChild(layout.findViewById(emojiTrayId)) + 1

            val ctx = layout.context
            val msg = (callFrame.args[0] as WidgetChatListActions.Model).message
            if (msg.channelId == THEMES_CHANNEL_ID) {
                val drawable = ContextCompat.getDrawable(ctx, R.d.ic_theme_24dp)?.mutate()?.apply {
                    setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))
                }

                HashMap<String, String>().apply {
                    msg.attachments.forEach {
                        if (it.url.endsWith(".json")) {
                            put(it.filename, it.url)
                        }
                    }
                    badUrlMatcher.matcher(msg.content).run {
                        while (find()) {
                            val url = group()
                            val name = url.substringAfterLast('/')
                            put(name, url)
                        }
                    }
                }.forEach { (name, url) ->
                    TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Icon).run {
                        this.id = id
                        val prettyName =
                            URLDecoder.decode(name, "UTF-8")
                                .replace('_', ' ')
                                .replace('-', ' ')
                                .removeSuffix(".json")

                        text = "Install $prettyName"
                        setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                        setOnClickListener {
                            Utils.threadPool.execute {
                                try {
                                    Http.Request(url).use {
                                        it.execute().saveToFile(File(THEME_DIR, name))
                                        ThemeLoader.loadThemes(false)
                                        Utils.showToast(ctx, "Successfully installed theme $prettyName")
                                    }
                                } catch (ex: Throwable) {
                                    logger.error(ctx, "Failed to install theme $prettyName", ex)
                                }
                            }
                        }

                        layout.addView(this, idx)
                    }
                }
            }
        })
}

private fun PatcherAPI.patchColorPicker() {
    /*
     * Discord does not use transparency so it does not get themed
     * This method is createPresetsView: https://github.com/discord/ColorPicker/blob/master/library/src/main/java/com/jaredrummler/android/colorpicker/ColorPickerDialog.java#L553
     * Wrapped into try catch so the plugin still works even if this method ever changes
     */
    try {
        patch(ColorPickerDialog::class.java.getDeclaredMethod("j"),
            PinePatchFn { cf ->
                val bundle = (cf.thisObject as ColorPickerDialog).arguments ?: return@PinePatchFn

                val view = cf.result as View
                val color = bundle.getInt("customButtonTextColor")
                val font = ResourcesCompat.getFont(view.context, bundle.getInt("buttonFont"))

                arrayOf(
                    com.jaredrummler.android.colorpicker.R.c.transparency_text,
                    com.jaredrummler.android.colorpicker.R.c.transparency_title
                ).forEach {
                    view.findViewById<TextView>(it)?.run {
                        setTextColor(color)
                        font?.let {
                            typeface = font
                        }
                    }
                }
            })
    } catch (th: Throwable) {
    }
}

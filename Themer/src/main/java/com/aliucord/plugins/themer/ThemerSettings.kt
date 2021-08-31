package com.aliucord.plugins.themer

import android.annotation.SuppressLint
import android.content.Intent
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aliucord.*
import com.aliucord.fragments.SettingsPage
import com.aliucord.plugins.Themer
import com.aliucord.utils.ChangelogUtils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.aliucord.views.Divider
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import com.lytefast.flexinput.R
import kotlin.system.exitProcess

class ThemerSettings : SettingsPage() {
    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        val ctx = view.context

        setActionBarTitle("Themer")

        TextView(ctx, null, 0, R.h.UiKit_TextView).run {
            val content = "Read the changelog!"
            SpannableStringBuilder(content).let {
                it.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val manifest = PluginManager.plugins["Themer"]!!.manifest
                        ChangelogUtils.show(context, manifest.version, manifest.changelogMedia, manifest.changelog)
                    }
                }, content.indexOf("changelog"), content.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = it
            }
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            DimenUtils.getDefaultPadding().let {
                setPadding(it, it, it, it)
            }
            movementMethod = LinkMovementMethod.getInstance()
            linearLayout.addView(this)
        }

        Button(ctx).apply {
            text = "Load missing themes"
            setOnClickListener {
                ThemeLoader.loadThemes(false)
                reRender()
            }

            linearLayout.addView(this)
        }

        TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Header).apply {
            text = "Transparency Mode"
            typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold)

            linearLayout.addView(this)
        }

        arrayListOf(
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "None", "No transparency"),
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "Chat", "Chat is transparent"),
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "Chat & Settings", "Chat and Settings page are transparent"),
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "Full", "Everything is transparent. NOT IMPLEMENTED YET"),
        ).let { radios ->
            val manager = RadioManager(radios)
            manager.a(radios[Themer.mSettings.transparencyMode.value])
            for (i in 0 until radios.size) {
                val radio = radios[i]
                radio.e {
                    manager.a(radio)
                    Themer.mSettings.transparencyMode = TransparencyMode.from(i)
                    promptRestart(view)
                }
                addView(radio)
            }
        }

        addView(Divider(ctx))

        TextView(ctx, null, 0, R.h.UiKit_Settings_Item_Header).apply {
            text = "Active Theme"
            typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold)

            linearLayout.addView(this)
        }

        if (ThemeLoader.themes.isEmpty()) {
            TextView(ctx, null, 0, R.h.UiKit_TextView).apply {
                text = "Hmm... No themes found."

                linearLayout.addView(this)
            }
        } else {
            val items = ThemeLoader.themes.map {
                Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.RADIO,
                    "${it.name} v${it.version}",
                    it.author
                )
            }
            val noTheme = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "None", null)
            (items as MutableList).add(0, noTheme)

            val manager = RadioManager(items)
            manager.a(noTheme)
            noTheme.e {
                ResourceManager.clean()
                val idx = manager.b()
                if (idx != 0) {
                    ThemeLoader.themes[idx - 1].disable()
                    manager.a(noTheme)
                    Utils.appActivity.recreate()
                    promptRestart(view)
                }
            }

            addView(noTheme)

            for (i in 1 until items.size) {
                val theme = ThemeLoader.themes[i - 1]
                val item = items[i]
                item.e {
                    if (manager.b() != i) {
                        if (theme.load()) {
                            theme.enable()
                            manager.a(item)
                            promptRestart(view)
                        } else {
                            Utils.showToast(ctx, "Something went wrong while loading that theme :(")
                        }
                    }
                }
                addView(item)
                if (theme.isEnabled) manager.a(item)
            }
        }
    }

    private fun promptRestart(v: View) {
        Snackbar.make(v, "Changes detected. Restart?", LENGTH_INDEFINITE)
            .setAction("Restart") {
                val ctx = it.context
                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.run {
                    startActivity(Intent.makeRestartActivityTask(component))
                    exitProcess(0)
                }
            }.show()
    }
}

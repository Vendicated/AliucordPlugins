/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.emojireplacer

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.PluginManager
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.settings.delegate
import com.aliucord.views.Divider
import com.discord.utilities.color.ColorCompat
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.lytefast.flexinput.R
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class Settings : SettingsPage() {
    private val mSettings = PluginManager.plugins["EmojiReplacer"]!!.settings

    private var activePack: String by mSettings.delegate("None")

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("Emoji Replacer")

        val ctx = view.context

        addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Active Emoji Pack"
        })

        val packs = ctx.installedPacks
        val items = packs.map {
            Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, it.name, Formatter.formatShortFileSize(ctx, it.folderSize))
        }.toMutableList()
        items.add(0, Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO, "None", null))

        val currIdx = if (activePack == "None") 0 else packs.indexOfFirst { it.name == activePack } + 1

        val manager = RadioManager(items)
        manager.a(items[currIdx])
        for (i in 0 until items.size) {
            val radio = items[i]
            radio.e {
                manager.a(radio)
                activePack = if (i == 0) "None" else
                    packs[i - 1].name
            }
            addView(radio)
        }

        addView(Divider(ctx))

        addView(TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Download Packs"
        })

        addView(RecyclerView(ctx).apply {
            adapter = PackAdapter(this@Settings)
            layoutManager = LinearLayoutManager(ctx)
        })
    }
}

data class Pack(val name: String, val downloadUrl: String, val size: Double)

class PackAdapter(val page: SettingsPage) : RecyclerView.Adapter<PackViewHolder>() {
    private var isDownloading = AtomicBoolean(false)
    private var packs = arrayOf(
        Pack("Apple", "https://cdn.discordapp.com/attachments/811261478875299840/917965536414036010/Apple.zip", 22.8),
        Pack("Facebook", "https://cdn.discordapp.com/attachments/811261478875299840/917965537957531648/Facebook.zip", 22.98),
        Pack("Google", "https://cdn.discordapp.com/attachments/811261478875299840/917965539295527002/Google.zip", 14.52),
        Pack("JoyPixels", "https://cdn.discordapp.com/attachments/811261478875299840/917965540360863824/JoyPixels.zip", 14.84),
        Pack("Microsoft", "https://cdn.discordapp.com/attachments/811261478875299840/917965541463969882/Microsoft.zip", 10.02),
        Pack("OpenMoji", "https://cdn.discordapp.com/attachments/811261478875299840/917965542185373736/OpenMoji.zip", 9.17),
        Pack("Samsung", "https://cdn.discordapp.com/attachments/811261478875299840/917965378221654066/Samsung.zip", 21.56),
        Pack("WhatsApp", "https://cdn.discordapp.com/attachments/811261478875299840/917965379685474354/WhatsApp.zip", 21.99),
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PackViewHolder(this, TextView(parent.context, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PackViewHolder, position: Int) {
        val (name, _, size) = packs[position]
        holder.textView.text = "$name ($size MB)"

        val ctx = holder.textView.context
        val icon = if (File(ctx.emojiDir, name).exists()) ContextCompat.getDrawable(ctx, R.e.ic_delete_24dp)?.apply {
            mutate()
            setTint(ContextCompat.getColor(ctx, R.c.uikit_btn_bg_color_selector_red))
        } else ContextCompat.getDrawable(ctx, R.e.ic_file_download_white_24dp)?.apply {
            mutate()
            setTint(ColorCompat.getThemedColor(ctx, R.b.colorInteractiveNormal))
        }

        holder.textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
    }

    fun onClick(ctx: Context, position: Int) {
        val (name, url) = packs[position]
        val dir = File(ctx.emojiDir, name)
        if (dir.exists()) {
            if (dir.deleteRecursively()) {
                Utils.showToast("Successfully deleted $name emojis")
                page.reRender()
            } else {
                Utils.showToast("Failed to delete $name emojis :(")
            }
        } else {
            if (isDownloading.get()) {
                Utils.showToast("Already downloading, relax pal...")
                return
            }
            Utils.showToast("Now downloading $name emojis...")
            isDownloading.set(true)
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val outFile = File(ctx.externalCacheDir, "$name.zip.tmp")
            outFile.deleteOnExit()
            PackReceiver(this, ctx, outFile, name).run {
                ctx.registerReceiver(this, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                downloadId = DownloadManager.Request(Uri.parse(url)).run {
                    setTitle("EmojiReplacer")
                    setDescription("Downloading $name emojis")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    setDestinationUri(Uri.fromFile(outFile))
                    dm.enqueue(this)
                }
            }
        }
    }

    override fun getItemCount() = packs.size

}

class PackViewHolder(private val adapter: PackAdapter, val textView: TextView) : RecyclerView.ViewHolder(textView) {
    init {
        textView.setOnClickListener { adapter.onClick(it.context, adapterPosition) }
    }
}

class PackReceiver(
    private val adapter: PackAdapter,
    private val ctx: Context,
    private val outFile: File,
    private val packName: String
) : BroadcastReceiver() {
    var downloadId = 0L

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                Utils.showToast("Successfully downloaded $packName emojis!")
                outFile.extract(File(context.emojiDir, packName))
                outFile.delete()
                adapter.page.reRender()
                ctx.unregisterReceiver(this)
            }
        } catch (th: Throwable) {
            logger.errorToast("Oops, something went wrong :(", th)
        }
    }
}

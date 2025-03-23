/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugs.checklinks

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.*
import androidx.viewbinding.ViewBinding
import com.aliucord.*
import com.aliucord.Http.QueryBuilder
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.Hook
import com.aliucord.utils.DimenUtils
import com.discord.app.AppDialog
import com.lytefast.flexinput.R
import java.lang.reflect.Method
import java.util.*

class MoreInfoModal(private val data: Map<String, Entry>) : SettingsPage() {
    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("URL info")

        val ctx = view.context
        val p = DimenUtils.defaultPadding
        val p2 = p / 2

        TableLayout(ctx).let { table ->
            for ((key, value) in data.toList().sortedBy { (_, value) -> value.result }.reversed()) {
                TableRow(ctx).let { row ->
                    TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
                        text = key
                        setPadding(p, p2, p, p2)
                        row.addView(this)
                    }
                    TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
                        text = value.result
                        setPadding(p, p2, p, p2)
                        row.addView(this)
                    }
                    table.addView(row)
                }
            }
            addView(table)
        }
    }
}

private fun makeReq(url: String, method: String, contentType: String, apiKey: String): Http.Request {
    return Http.Request(url, method).apply {
        setHeader("Content-Type", contentType)
        setHeader("User-Agent", "Aliucord Plugin") // More appropriate user agent.
        setHeader("x-apikey", apiKey) // Add the API key from settings
    }
}

private fun checkLink(url: String, apiKey: String): Map<String, Entry> {
    // Check if API key is provided
    if (apiKey.isEmpty() || apiKey.trim().isEmpty()) {
        throw Exception("No VirusTotal API key provided. Please set it in the plugin settings.")
    }

    // Look up url in cache first
    val analysisId = makeReq("https://www.virustotal.com/api/v3/urls", "POST", "application/x-www-form-urlencoded", apiKey)
        .executeWithUrlEncodedForm(mapOf("url" to url))
        .json(UrlIdInfo::class.java).data.id

    return makeReq("https://www.virustotal.com/api/v3/analyses/$analysisId", "GET", "application/json", apiKey)
        .execute()
        .json(NewUrlInfo::class.java)
        .data.attributes.results
}

class CheckLinksSettings : SettingsPage() {
    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("CheckLinks Settings")

        val ctx = view.context
        val plugin = PluginManager.plugins["CheckLinks"] as CheckLinks

        com.aliucord.views.TextInput(ctx, "VirusTotal API Key").run {
            editText.run {
                maxLines = 1
                setText(plugin.settings.getString("virusTotalApiKey", ""))
                hint = "Enter your VirusTotal API key here"

                addTextChangedListener(object : com.discord.utilities.view.text.TextWatcher() {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable) {
                        plugin.settings.setString("virusTotalApiKey", s.toString())
                    }
                })
            }

            linearLayout.addView(this)
        }

        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_SubText).apply {
            text = "You can get a free API key from virustotal.com. The plugin will not work without a valid API key."
            linearLayout.addView(this)
        }
    }
}

@AliucordPlugin
class CheckLinks : Plugin() {
    init {
        settingsTab = SettingsTab(CheckLinksSettings::class.java)
    }

    @SuppressLint("SetTextI18n")
    override fun start(ctx: Context) {
        var getBinding: Method? = null

        val dialogTextId = Utils.getResId("masked_links_body_text", "id")

        patcher.patch(
            b.a.a.g.a::class.java.getMethod("onViewBound", View::class.java),
            Hook { param ->
                val dialog = param.thisObject as AppDialog
                val url = dialog.arguments?.getString("WIDGET_SPOOPY_LINKS_DIALOG_URL")
                    ?: return@Hook

                if (getBinding == null) {
                    b.a.a.g.a::class.java.declaredMethods.find {
                        ViewBinding::class.java.isAssignableFrom(it.returnType)
                    }?.let {
                        Logger("CheckLinks").info("Found obfuscated getBinding(): ${it.name}()")
                        getBinding = it
                    } ?: run {
                        Logger("CheckLinks").error("Couldn't find obfuscated getBinding()", null)
                        return@Hook
                    }
                }
                val binding = getBinding!!.invoke(dialog) as ViewBinding
                val text = binding.root.findViewById<TextView>(dialogTextId)
                text.text = "Checking URL $url..."

                Utils.threadPool.execute {
                    var content: String
                    var data: Map<String, Entry>? = null

                    try {
                        // Get API key from settings
                        val apiKey = settings.getString("virusTotalApiKey", "")

                        // Check if API key is provided
                        if (apiKey.isEmpty() || apiKey.trim().isEmpty()) {
                            throw Exception("No VirusTotal API key provided. Please set it in the plugin settings.")
                        }

                        data = checkLink(url, apiKey)

                        val counts = IntArray(4)
                        data.values.forEach { v ->
                            when (v.result) {
                                "clean" -> counts[0]++
                                "phishing" -> counts[1]++
                                "malicious" -> counts[2]++
                                else -> counts[3]++
                            }
                        }

                        val malicious = counts[1] + counts[2]
                        content =
                            if (malicious > 0)
                                "URL $url is ${if (malicious > 2) "likely" else "possibly"} malicious. $malicious engines flagged it as malicious."
                            else
                                "URL $url is either safe or too new to be flagged."
                    } catch (th: Throwable) {
                        Logger("[CheckLinks]").error("Failed to check link $url", th)
                        content = "Failed to check URL $url: ${th.message ?: "Unknown error"}. " +
                                "Please check your VirusTotal API key in settings."
                    }

                    if (data != null) content += "\n\nMore Info"

                    SpannableString(content).run {
                        val urlIdx = content.indexOf(url)
                        if (urlIdx >= 0) {
                            setSpan(URLSpan(url), urlIdx, urlIdx + url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }

                        data?.let {
                            val moreInfoIdx = content.lastIndexOf("More Info")
                            if (moreInfoIdx >= 0) {
                                setSpan(object : ClickableSpan() {
                                    override fun onClick(view: View) {
                                        Utils.openPageWithProxy(view.context, MoreInfoModal(it))
                                    }
                                }, moreInfoIdx, moreInfoIdx + 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        }

                        Utils.mainThread.post {
                            text.movementMethod = LinkMovementMethod.getInstance()
                            text.text = this
                        }
                    }
                }
            })
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}

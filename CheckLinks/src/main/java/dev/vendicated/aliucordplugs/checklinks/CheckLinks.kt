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
import dev.vendicated.aliucordplugs.checklinks.*
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

<<<<<<< HEAD
private fun makeReq(url: String, method: String, contentType: String): Http.Request {
    val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    val s = CharArray(10) { chars.random() }.joinToString("")

    return Http.Request(url, method).apply {
        setHeader("Content-Type", contentType)
        setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:83.0) Firefox")
        setHeader("X-Tool", "vt-ui-main")
        setHeader("X-VT-Anti-Abuse-Header", s) // Can be anything for some reason
        setHeader("Accept-Ianguage", "en-US,en;q=0.9,es;q=0.8") // yes upper case i lol
    }
}

private fun checkLink(url: String): Map<String, Entry> {
    // Look up url in cache first
    QueryBuilder("https://www.virustotal.com/ui/search").run {
        append("limit", "20")
        append("relationships[comment]", "author,item")
        append("query", url)

        makeReq(this.toString(), "GET", "application/json")
            .execute()
            .json(CachedUrlInfo::class.java)
            .let { res ->
                if (res.data.isNotEmpty()) return@checkLink res.data[0].attributes.last_analysis_results
            }
    }

    // no cached data, make full request for url

    // R.h.ster url to get an ID
    val idInfo =
        makeReq("https://www.virustotal.com/ui/urls", "POST", "application/x-www-form-urlencoded")
            .executeWithUrlEncodedForm(mapOf("url" to url))
            .json(UrlIdInfo::class.java)

    // Request analysis with that ID
    return makeReq(
        "https://www.virustotal.com/ui/analyses/" + idInfo.data.id,
        "GET",
        "application/json"
    )
=======
// Settings class for CheckLinks plugin with proper constructor
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
>>>>>>> d08621c (Update CheckLinks.kt)
        .execute()
        .json(NewUrlInfo::class.java)
        .data.attributes.results
}

<<<<<<< HEAD

@AliucordPlugin
class CheckLinks : Plugin() {
=======
@AliucordPlugin
class CheckLinks : Plugin() {
    init {
        settingsTab = SettingsTab(CheckLinksSettings::class.java)
    }
    
>>>>>>> d08621c (Update CheckLinks.kt)
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
<<<<<<< HEAD

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
                        data = checkLink(url)

=======

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

>>>>>>> d08621c (Update CheckLinks.kt)
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
<<<<<<< HEAD
                        content = "Failed to check URL $url. Proceed at your own risk."
=======
                        content = "Failed to check URL $url: ${th.message ?: "Unknown error"}. " +
                                "Please check your VirusTotal API key in settings."
>>>>>>> d08621c (Update CheckLinks.kt)
                    }

                    if (data != null) content += "\n\nMore Info"

                    SpannableString(content).run {
                        val urlIdx = content.indexOf(url)
<<<<<<< HEAD
                        setSpan(URLSpan(url), urlIdx, urlIdx + url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                        data?.let {
                            setSpan(object : ClickableSpan() {
                                override fun onClick(view: View) {
                                    Utils.openPageWithProxy(view.context, MoreInfoModal(it))
                                }
                            }, content.length - 9, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }

=======
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

>>>>>>> d08621c (Update CheckLinks.kt)
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

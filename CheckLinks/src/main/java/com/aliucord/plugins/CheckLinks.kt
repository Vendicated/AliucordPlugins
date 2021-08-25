/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/
package com.aliucord.plugins

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.*
import androidx.viewbinding.ViewBinding
import com.aliucord.*
import com.aliucord.Http.QueryBuilder
import com.aliucord.entities.Plugin
import com.aliucord.entities.Plugin.Manifest.Author
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.PinePatchFn
import com.aliucord.utils.DimenUtils
import com.discord.app.AppDialog
import com.lytefast.flexinput.R
import java.io.IOException
import java.lang.reflect.Method
import java.util.*

class CheckLinks : Plugin() {
    class MoreInfoModal(private val data: Map<String, CachedUrlInfo.Data.Attributes.Entry>) : SettingsPage() {
        override fun onViewBound(view: View) {
            super.onViewBound(view)
            setActionBarTitle("URL info")
            val ctx = view.context
            val p = DimenUtils.getDefaultPadding()
            val p2 = p / 2
            TableLayout(ctx).apply {
                for ((key, value) in data.toList().sortedBy { (_, value) -> value.result }) {
                    TableRow(ctx).let { row ->
                        TextView(ctx, null, 0, R.h.UiKit_TextView).apply {
                            text = key
                            setPadding(p, p2, p, p2)
                            row.addView(this)
                        }
                        TextView(ctx, null, 0, R.h.UiKit_TextView).apply {
                            text = value.result
                            setPadding(p, p2, p, p2)
                            row.addView(this)
                        }
                        this.addView(row)
                    }
                }
                addView(this)
            }
        }
    }

    override fun getManifest() = Manifest().apply {
        authors = arrayOf(Author("Vendicated", 343383572805058560L))
        description = "Checks links via the VirusTotal api"
        version = "1.0.4"
        updateUrl = "https://raw.githubusercontent.com/Vendicated/AliucordPlugins/builds/updater.json"
    }

    @Throws(Throwable::class)
    override fun start(ctx: Context) {
        var getBinding: Method? = null

        val dialogTextId = Utils.getResId("masked_links_body_text", "id")
        patcher.patch(c.a.a.g.a::class.java.getMethod("onViewBound", View::class.java), PinePatchFn { callFrame ->
            val dialog = callFrame.thisObject as AppDialog
            val url = dialog.arguments?.getString("WIDGET_SPOOPY_LINKS_DIALOG_URL") ?: return@PinePatchFn
            if (getBinding == null) {
                c.a.a.g.a::class.java.declaredMethods.find {
                    ViewBinding::class.java.isAssignableFrom(it.returnType)
                }?.let {
                    Logger("CheckLinks").info("Found obfuscated getBinding(): ${it.name}()")
                    getBinding = it
                } ?: run {
                    Logger("CheckLinks").error("Couldn't find obfuscated getBinding()", null)
                    return@PinePatchFn
                }
            }
            val binding = getBinding!!.invoke(dialog) as ViewBinding
            val text = binding.root.findViewById<TextView>(dialogTextId)
            text.text = String.format("Checking URL %s...", url)
            Utils.threadPool.execute {
                var content: String
                var data: Map<String, CachedUrlInfo.Data.Attributes.Entry>? = null
                try {
                    data = checkLink(url)
                    content = data?.let {
                        val counts = IntArray(4)
                        it.values.forEach { v ->
                            when (v.result) {
                                "clean" -> counts[0]++
                                "phishing" -> counts[1]++
                                "malicious" -> counts[2]++
                                else -> counts[3]++
                            }
                        }
                        val malicious = counts[1] + counts[2]
                        when {
                            malicious > 2 -> String.format("URL %%s is likely malicious. %s engines flagged it.", malicious)
                            malicious > 0 -> String.format("URL %%s is possibly malicious. %s engines flagged it.", malicious)
                            else -> "URL %s is either safe or too new to be flagged."
                        }
                    } ?: "No info on URL %s. Proceed at your own risk."
                } catch (th: Throwable) {
                    Log.e("[CheckLinks]", "Oops", th)
                    content = "Failed to check URL %s. Proceed at your own risk."
                }
                content = String.format(content, url)
                if (data != null) content += "\n\nMore Info"

                val spannableContent = SpannableString(content).apply {
                    val urlIdx = content.indexOf(url)
                    setSpan(object : ClickableSpan() {
                        override fun onClick(view: View) =
                                Intent(Intent.ACTION_VIEW).let {
                                    it.data = Uri.parse(url)
                                    dialog.startActivity(it)
                                }
                    }, urlIdx, urlIdx + url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    data?.let {
                        setSpan(object: ClickableSpan() {
                            override fun onClick(view: View) {
                                Utils.openPageWithProxy(view.context, CheckLinks.MoreInfoModal(it))
                            }
                        }, content.length - 9, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                Utils.mainThread.post {
                    text.movementMethod = LinkMovementMethod.getInstance()
                    text.text = spannableContent
                }
            }
        })
    }

    class CachedUrlInfo {
        lateinit var data: List<Data>

        class Data {
            lateinit var attributes: Attributes

            class Attributes {
                lateinit var last_analysis_results: Map<String, Entry>

                class Entry {
                    lateinit var result: String
                }
            }
        }
    }

    class NewUrlInfo {
        lateinit var data: Data

        class Data {
            lateinit var attributes: Attributes

            class Attributes {
                lateinit var results: Map<String, CachedUrlInfo.Data.Attributes.Entry>
            }
        }
    }

    class UrlIdInfo {
        lateinit var data: Data

        class Data {
            lateinit var id: String
        }
    }

    @Throws(IOException::class)
    private fun makeReq(url: String, method: String, contentType: String) =
            Http.Request(url, method).apply {
                setHeader("Content-Type", contentType)
                setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:83.0) Firefox")
                setHeader("X-Tool", "vt-ui-main") // Can be anything for some reason
                setHeader("X-VT-Anti-Abuse-Header", "uwu") // yes upper case i lol
                setHeader("Accept-Ianguage", "en-US,en;q=0.9,es;q=0.8")
            }

    @Throws(IOException::class)
    private fun checkLink(url: String?): Map<String, CachedUrlInfo.Data.Attributes.Entry>? {
        val qb = QueryBuilder("https://www.virustotal.com/ui/search")
                .append("limit", "20")
                .append("relationships[comment]", "author,item")
                .append("query", url)

        val cached = makeReq(qb.toString(), "GET", "application/json")
                .execute()
                .json(CachedUrlInfo::class.java)

        if (cached.data.isNotEmpty()) return cached.data[0].attributes.last_analysis_results

        // no cached data, make full request
        val idInfo = makeReq("https://www.virustotal.com/ui/urls", "POST", "application/x-www-form-urlencoded")
                .executeWithUrlEncodedForm(mapOf("url" to url))
                .json(UrlIdInfo::class.java)

        return makeReq("https://www.virustotal.com/ui/analyses/" + idInfo.data.id, "GET", "application/json")
                .execute()
                .json(NewUrlInfo::class.java)
                .data.attributes.results
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
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
        .execute()
        .json(NewUrlInfo::class.java)
        .data.attributes.results
}


@AliucordPlugin
class CheckLinks : Plugin() {
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
                        data = checkLink(url)

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
                        content = "Failed to check URL $url. Proceed at your own risk."
                    }

                    if (data != null) content += "\n\nMore Info"

                    SpannableString(content).run {
                        val urlIdx = content.indexOf(url)
                        setSpan(URLSpan(url), urlIdx, urlIdx + url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                        data?.let {
                            setSpan(object : ClickableSpan() {
                                override fun onClick(view: View) {
                                    Utils.openPageWithProxy(view.context, MoreInfoModal(it))
                                }
                            }, content.length - 9, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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

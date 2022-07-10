/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.jseval

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils.dp
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lytefast.flexinput.R
import java.util.concurrent.atomic.AtomicReference

fun String.unescape(): String {
    return replace("\\\\[Uu]([0-9A-Fa-f]{4})".toRegex()) {
        it.groupValues[1].toInt(16).toChar().toString()
    }.replace("\\n", "\n")
}

@SuppressLint("SetJavaScriptEnabled") // Cope LMAO
class TerminalPage : SettingsPage() {
    companion object {
        private val historyAdapter = TerminalHistoryAdapter()
        const val HELP_MESSAGE = """
Welcome! :)
Commands:
    .help            - Get help
    .clear           - Clear the terminal
    .reset           - Reset all variables
    .stop            - Abort the interpreter in case it gets stuck :)
    .themes          - List the available themes
    .theme [THEME]   - Set the theme

"""
    }

    private lateinit var webView: WebView
    private lateinit var recyclerView: RecyclerView
    private var pendingResult = AtomicReference(false)

    private fun initWebView(ctx: Context) {
        webView = WebView(ctx).apply {
            settings.javaScriptEnabled = true
        }
    }

    private fun append(item: HistoryItem) {
        val position = historyAdapter.append(item)
        recyclerView.post {
            recyclerView.smoothScrollToPosition(position)
        }
    }

    private val screenHeight: Int by lazy {
        val wm = Utils.appActivity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getSize(p)
        val leMagicNumber = 90.dp
        p.x.coerceAtLeast(p.y) - leMagicNumber - headerBar.height - headerBar.paddingBottom - headerBar.paddingTop
    }

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("JS Shell")

        val ctx = view.context
        val p = 16.dp
        setPadding(p)
        initWebView(ctx)

        linearLayout.gravity = Gravity.CENTER_VERTICAL
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setPadding(p, p, p, p)
            setBackgroundColor(Color.BLACK)
            linearLayout.addView(this)
        }

        recyclerView = RecyclerView(ctx).apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(ctx)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                weight = 1f
            }
            root.addView(this)
        }

        var editText: TextInputEditText
        TextInputLayout(ctx).run {
            editText = TextInputEditText(ctx).apply {
                typeface = ResourcesCompat.getFont(ctx, Constants.Fonts.sourcecodepro_semibold)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                ContextCompat.getDrawable(ctx, R.e.ic_arrow_right_24dp)!!.run {
                    mutate()
                    setTint(Color.WHITE)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(this, null, null, null)
                }
            }.also { addView(it) }

            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            editText.setOnKeyListener { _, keycode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_ENTER) {
                    when (val text = editText.text.toString().trim()) {
                        ".clear" -> historyAdapter.clear()
                        ".stop" -> {
                            append(EmptyItem("I actually dont know how to stop an infinite loop so please restart the app lol", false))
                            webView.loadUrl("about:blank")
                        }
                        ".reset" -> {
                            webView.loadUrl("about:blank")
                            append(EmptyItem("RESET", true))
                        }
                        ".help" -> {
                            append(EmptyItem(HELP_MESSAGE, false))
                        }
                        ".themes" -> {
                            append(EmptyItem("Themes aren't implemented yet ;)", false))
                        }
                        else -> {
                            eval(text) || return@setOnKeyListener true
                        }
                    }
                    editText.text!!.clear()
                }
                true
            }
            root.addView(this)
        }

        LinearLayout(ctx).run {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = p
            }
            setPadding(p, p / 2, p, p / 2)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.BLACK)

            addView(TerminalButton(ctx, R.e.ic_help_24dp) {
                append(EmptyItem(HELP_MESSAGE, false))
            })
            addView(TerminalButton(ctx, R.e.ic_arrow_up_24dp) {
                append(EmptyItem(HELP_MESSAGE, false))
            })
            addView(TerminalButton(ctx, R.e.ic_arrow_down_24dp) {
                append(EmptyItem(HELP_MESSAGE, false))
            })
            root.addView(this)
        }

        // Welcome message
        if (historyAdapter.itemCount == 0)
            append(EmptyItem(HELP_MESSAGE, false))
    }

    private fun eval(code: String): Boolean {
        if (pendingResult.getAndSet(true)) {
            Utils.showToast("Wait for the previous code to finish executing or run stop")
            return false
        }

        val script = """
            try {
                ${code.replace("\\b(let|const)\\b".toRegex(), "var")} // var is hoisted so it breaks try catch block and becomes global
            } catch (err) {
                err.stack || err.message || "An error occurred"
            }
        """
        webView.evaluateJavascript(script) {
            val result = when (it) {
                "null" -> "undefined"
                else -> it.unescape()
            }
            append(ResultItem(code, result, result.contains("Error:") && result.contains("anonymous>")))
            pendingResult.set(false)
        }
        return true
    }
}

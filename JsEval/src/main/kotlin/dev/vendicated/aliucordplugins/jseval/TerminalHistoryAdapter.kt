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
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lytefast.flexinput.R

abstract class HistoryItem(val viewType: Int)
enum class ViewType {
    RESULT, EMPTY
}
data class ResultItem(val input: String, val output: String, val isError: Boolean) : HistoryItem(ViewType.RESULT.ordinal)
data class EmptyItem(val text: String, val isError: Boolean) : HistoryItem(ViewType.EMPTY.ordinal)

class TerminalHistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val data = ArrayList<HistoryItem>()

    override fun getItemViewType(position: Int) = data[position].viewType

    override fun getItemCount() = data.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            ViewType.RESULT.ordinal -> ResultEntry.create(parent)
            ViewType.EMPTY.ordinal -> EmptyEntry.create(parent)
            else -> throw NotImplementedError()
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = data[position]) {
            is ResultItem -> {
                (holder as ResultEntry).bindView(item.input, item.output, item.isError)
            }
            is EmptyItem -> {
                (holder as EmptyEntry).bindView(item.text, item.isError)
            }
        }
    }

    fun append(item: HistoryItem): Int {
        data.add(item)
        notifyItemInserted(data.size - 1)
        return data.size - 1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }
}

class EmptyEntry(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
    fun bindView(text: String, isError: Boolean) {
        textView.text = text
        textView.setTextColor(if (isError) Color.RED else Color.WHITE)
    }

    companion object {
        fun create(parent: ViewGroup): EmptyEntry {
            val textView = CodeTextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                // textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            return EmptyEntry(textView)
        }
    }
}

class ResultEntry(layout: LinearLayout, private val promptView: TextView, private val resultView: TextView) : RecyclerView.ViewHolder(layout) {
    fun bindView(input: String, output: String, isError: Boolean) {
        promptView.text = input
        resultView.text = output
        resultView.setTextColor(if (isError) Color.RED else Color.WHITE)
    }

    companion object {
        fun create(parent: ViewGroup): ResultEntry {
            val ctx = parent.context
            val fullWidthParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = fullWidthParams
            }
            val promptView = CodeTextView(ctx).apply {
                ContextCompat.getDrawable(ctx, R.e.ic_arrow_right_24dp)!!.run {
                    mutate()
                    setTint(Color.WHITE)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(this, null, null, null)
                }
                layoutParams = fullWidthParams
                this.gravity = Gravity.CENTER_VERTICAL
            }
            val resultView = CodeTextView(ctx).apply {
                layoutParams = fullWidthParams
                // Add drawable with same colour as background to properly align text
                ContextCompat.getDrawable(ctx, R.e.ic_arrow_right_24dp)!!.run {
                    mutate()
                    setTint(Color.BLACK)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(this, null, null, null)
                }
            }
            return ResultEntry(layout, promptView, resultView).also {
                layout.addView(promptView)
                layout.addView(resultView)
            }
        }
    }
}

/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.textfilepreview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.settings.delegate
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.filename
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.size
import com.aliucord.wrappers.messages.AttachmentWrapper.Companion.url
import com.discord.api.message.attachment.MessageAttachment
import com.discord.utilities.color.ColorCompat
import com.discord.utilities.file.FileUtilsKt
import com.discord.utilities.io.NetworkUtils
import com.lytefast.flexinput.R

val previewWidgetId = View.generateViewId()

@SuppressLint("SetTextI18n")
class AttachmentPreviewWidget(ctx: Context, private val attachment: MessageAttachment, private val settings: SettingsAPI) : LinearLayout(ctx),
    View.OnClickListener {
    private val previewSize: Int by settings.delegate(300)

    private var mContentPreview = null as String?
    private var mFullContent = null as String?
    private var mExpanded = false
    private var mHidden = false

    private val canExpand = attachment.size > 1000

    private val mTextView: TextView
    private val mFooterLayout: LinearLayout
    private val mFilenameView: TextView

    init {
        val dp8 = 8.dp
        val dp16 = 16.dp
        this.id = previewWidgetId
        orientation = VERTICAL

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setOnClickListener(this)

        mTextView = TextView(ctx, null, 0, R.i.UiKit_TextView).apply {
            text = "Loading..."
            setPadding(dp16, dp16, dp16, dp16)
            setTextIsSelectable(true)
        }

        mFooterLayout = LinearLayout(ctx).apply {
            setPadding(dp8, dp8, dp8, dp8)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            setBackgroundColor(ColorCompat.getThemedColor(ctx, R.b.colorBackgroundSecondaryAlt))
            mFilenameView = TextView(ctx, null, 0, R.i.UiKit_TextView_Bold).apply {
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                    weight = 1f
                }

                text = "${attachment.filename} (${FileUtilsKt.getSizeSubtitle(attachment.size)})"
                addView(this)
            }

            addView(makeImageView(R.e.ic_copy_24dp, "Download", 4.dp) {
                if (mExpanded && mFullContent != null ) {
                    Utils.setClipboard("content", mFullContent!!)
                    Utils.showToast("Copied to clipboard!")
                } else if (mContentPreview != null) {
                    Utils.setClipboard("content", mContentPreview!!)
                    Utils.showToast("Partial content copied. Expand the preview to copy the full content.")
                }
            })

            addView(makeImageView(R.e.ic_file_download_white_24dp, "Download", 4.dp) {
                alpha = 0.5f
                isEnabled = false
                val commonCallback = { _: String? ->
                    alpha = 1f
                    isEnabled = true
                }
                NetworkUtils.downloadFile(context, Uri.parse(attachment.url), attachment.filename, null, commonCallback) {
                    logger.errorToast("Failed to download ${attachment.filename}", it)
                    commonCallback(null)
                }
            })

            addView(makeImageView(R.e.ic_visibility_white_24dp, "Close preview") {
                it as ImageView
                if (mHidden) {
                    mTextView.visibility = View.VISIBLE
                    it.setImageDrawable(getThemedDrawable(R.e.ic_visibility_white_24dp))
                } else {
                    mTextView.visibility = View.GONE
                    it.setImageDrawable(getThemedDrawable(R.e.ic_visibility_off_white_a60_24dp))
                }
                mHidden = !mHidden
            })
        }
        addView(mTextView)
        addView(mFooterLayout)

        Utils.threadPool.execute {
            try {
                mContentPreview = Http.Request(attachment.url).use {
                    it.setHeader("Range", "bytes=0-$previewSize") // Only download first 1Kb to limit data usage
                    it.execute().text()
                }
            } catch (th: Throwable) {
                logger.error(th)
            }
            Utils.mainThread.post {
                configure()
            }
        }
    }

    private fun configure() {
        TransitionManager.beginDelayedTransition(mTextView.parent as ViewGroup)

        val text = if (mExpanded) mFullContent.let {
            if (it == null) {
                Utils.threadPool.execute {
                    try {
                        mFullContent = Http.simpleGet(attachment.url)
                        Utils.mainThread.post {
                            configure()
                        }
                    } catch (th: Throwable) {
                        logger.error(th)
                    }
                }
                "Loading full content..."
            } else it
        } else mContentPreview?.take(1000)
        mTextView.text = text ?: "Failed to load :("

        if (canExpand) {
            val expandDrawable = ContextCompat.getDrawable(context, R.e.ic_arrow_down_24dp)!!.mutate().apply {
                setTint(ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal))
            }
            var drawable = expandDrawable;
            if (mExpanded) {
                drawable = object : LayerDrawable(arrayOf(expandDrawable)) {
                    override fun draw(canvas: Canvas) {
                        val bounds = expandDrawable.bounds
                        canvas.save()
                        canvas.rotate(180f, bounds.width() / 2f, bounds.height() / 2f)
                        super.draw(canvas)
                        canvas.restore()
                    }
                }
            }
            mFilenameView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        }
    }

    override fun onClick(view: View) {
        if (attachment.size > 1000 * 500) {
            Utils.showToast("That file is too large. Please download it to view it fully.")
        } else if (canExpand && !mHidden) {
            Utils.mainThread.post {
                mExpanded = !mExpanded
                configure()
            }
        }
    }

    private fun makeImageView(drawableId: Int, accessibilityText: String, marginRight: Int = 0, onClick: OnClickListener) = ImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.END
            marginEnd = marginRight
        }
        setImageDrawable(getThemedDrawable(drawableId))
        contentDescription = accessibilityText // I LOVE Accessibility

        setOnClickListener(onClick)
    }

    private fun getThemedDrawable(drawableId: Int) = ContextCompat.getDrawable(context, drawableId)!!.mutate().apply {
        setTint(ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal))
    }
}

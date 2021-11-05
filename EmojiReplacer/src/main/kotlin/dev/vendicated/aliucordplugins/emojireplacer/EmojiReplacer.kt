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

import android.content.Context
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.utils.IOUtils
import com.discord.models.domain.emoji.ModelEmojiUnicode
import com.discord.stores.StoreStream
import java.io.File
import java.util.zip.ZipFile

@AliucordPlugin
class EmojiReplacer : Plugin() {
    private val logger = Logger("EmojiReplacer")

    override fun start(ctx: Context) {
        loadEmojiZip(File("/sdcard/Download/noto.zip"))
        patcher.patch(ModelEmojiUnicode::class.java.getDeclaredMethod("getImageUri", String::class.java, Context::class.java), Hook { param ->
            emojis[param.args[0]]?.let {
                param.result = it
            }
        })
    }

    private val emojis = HashMap<String, String>()

    private fun loadEmojiZip(zip: File) {
        emojis.clear()
        val surrogateMap = StoreStream.getEmojis().unicodeEmojiSurrogateMap
        val emojiDir = File(Utils.appContext.dataDir, "EmojiReplacer/${zip.nameWithoutExtension}")
        val dirname = emojiDir.absolutePath
        emojiDir.mkdirs()

        ZipFile(zip).use { zipFile ->
            for (entry in zipFile.entries()) {
                val fileName = entry.name.substringAfterLast('/')
                if (fileName.endsWith(".png")) {
                    val split = fileName.removeSuffix(".png").split('_')
                    val surrogate = split.first()
                    var emoji = surrogateMap[surrogate]
                    if (split.size > 1 && emoji != null) {
                        emoji = emoji.diversityChildren?.find { it.surrogates == split[1] }
                    }
                    if (emoji == null) {
                        logger.warn("Unrecognised emoji $fileName")
                        continue
                    }

                    zipFile.getInputStream(entry).use { eis ->
                        File(emojiDir, fileName).outputStream().use { fos ->
                            IOUtils.pipe(eis, fos)
                            emojis[emoji.codePoints] = "file://$dirname/$fileName"
                        }
                    }
                }
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
    }
}

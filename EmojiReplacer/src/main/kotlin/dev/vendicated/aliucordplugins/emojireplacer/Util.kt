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
import java.io.File
import java.io.FileNotFoundException
import java.util.zip.ZipFile

fun File.extract(outputDir: File) {
    if (!outputDir.exists() && !outputDir.mkdirs()) throw RuntimeException("Failed to create directory $outputDir")

    ZipFile(this).use { zip ->
        zip.entries().asSequence().forEach {
            val isDir = it.isDirectory
            val file = File(outputDir, it.name)
            val dir = if (isDir) file else file.parentFile
            if (!dir.isDirectory && !dir.mkdirs()) throw RuntimeException("Failed to create directory $outputDir")
            if (!isDir) {
                zip.getInputStream(it).use { zis -> zis.copyTo(file.outputStream()) }
            }
        }
    }
}

val Context.emojiDir
    get() = File(filesDir, "emoji-replacer")

val Context.installedPacks: Array<File>
    get() = emojiDir.run {
        if (!exists() && !mkdirs()) throw FileNotFoundException()
        listFiles()!!
    }


val File.folderSize: Long
    get() {
        var size = 0L
        for (file in listFiles()!!) {
            size += if (file.isDirectory) file.folderSize
            else file.length()
        }
        return size
    }

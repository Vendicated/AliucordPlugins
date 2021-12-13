/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.aliueval

import android.content.Context
import com.aliucord.Http
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.aliucord.utils.*
import dalvik.system.DexClassLoader
import java.io.File
import java.util.*

class Code(val code: String)
class CompileError(val stdout: String, val stderr: String)

@AliucordPlugin
class AliuEval : Plugin() {
    override fun start(ctx: Context) {
        commands.registerCommand("eval", "evaluate kotlin", CommandsAPI.requiredMessageOption) {
            try {
                val code = it.getRequiredString("message")
                val outFile = File(ctx.codeCacheDir, "eval.dex")
                Http.Request("https://aliueval.vendicated.dev", "POST").use {
                    it.executeWithJson(Code(code)).saveToFile(outFile)
                }
                val cl = DexClassLoader(outFile.absolutePath, ctx.codeCacheDir.absolutePath, null, this.javaClass.classLoader)
                val clazz = cl.loadClass("dev.vendicated.aliucordeval.Eval")
                val instance = ReflectUtils.invokeConstructorWithArgs(clazz, patcher, settings, commands)
                val ret = ReflectUtils.invokeMethod(instance, "main")
                try {
                    CommandsAPI.CommandResult("```json\n${GsonUtils.toJson(ret)}```", null, false)
                } catch (th: Throwable) {
                    CommandsAPI.CommandResult("```${Objects.toString(ret)}```", null, false)
                }
            } catch (th: Throwable) {
                val msg = if (th is Http.HttpException && th.statusCode == 400) {
                    val err = GsonUtils.fromJson(th.req.conn.errorStream.use { IOUtils.readAsText(it) }, CompileError::class.java)
                    "STDOUT: ```\n${err.stdout + " "}```\nSTDERR: ```\n${err.stderr}```"
                } else "```\n${th.message}```"
                CommandsAPI.CommandResult(msg, null, false)
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
    }
}

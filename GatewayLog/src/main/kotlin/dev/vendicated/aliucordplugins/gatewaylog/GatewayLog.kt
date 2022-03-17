/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package dev.vendicated.aliucordplugins.gatewaylog

import android.content.Context
import com.aliucord.Constants
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.utils.ReflectUtils
import com.discord.gateway.GatewaySocketLogger
import com.discord.stores.StoreStream
import com.discord.utilities.logging.AppGatewaySocketLogger
import java.io.File
import java.io.FileOutputStream

@AliucordPlugin
class GatewayLog : Plugin() {
    private lateinit var inbound: FileOutputStream
    private lateinit var outbound: FileOutputStream

    override fun start(ctx: Context) {
        val base = File(Constants.BASE_PATH)
        inbound = FileOutputStream(File(base, "gateway_inbound.txt"))
        outbound = FileOutputStream(File(base, "gateway_outbound.txt"))

        ReflectUtils.setField(ReflectUtils.getField(StoreStream.getGatewaySocket(), "socket")!!, "gatewaySocketLogger", object : GatewaySocketLogger {
            override fun getLogLevel(): GatewaySocketLogger.LogLevel {
                return GatewaySocketLogger.LogLevel.VERBOSE
            }
            override fun logInboundMessage(str: String) {
                inbound.write((str + "\n").toByteArray())
            }
            override fun logMessageInflateFailed(th: Throwable) {
                logger.error("Error inflating gateway message (Exception comes from Discord)", th)
            }
            override fun logOutboundMessage(str: String) {
                outbound.write((str + "\n").toByteArray())
            }
        })
    }

    override fun stop(context: Context) {
        ReflectUtils.setField(ReflectUtils.getField(StoreStream.getGatewaySocket(), "socket")!!, "gatewaySocketLogger", (AppGatewaySocketLogger.Companion).instance)
        inbound.close()
        outbound.close()
    }
}

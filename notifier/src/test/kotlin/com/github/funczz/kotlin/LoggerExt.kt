package com.github.funczz.kotlin

import java.util.logging.LogManager
import java.util.logging.Logger

fun <T> Class<T>.getJULLogger(): Logger {
    val prop = """
    |handlers= java.util.logging.ConsoleHandler
    |.level= INFO
    |java.util.logging.ConsoleHandler.level = INFO
    |java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
    |java.util.logging.SimpleFormatter.format=%1${'$'}tY-%1${'$'}tm-%1${'$'}td %1${'$'}tH:%1${'$'}tM:%1${'$'}tS.%1${'$'}tL %2${'$'}s%n[%4${'$'}s] %5${'$'}s%6${'$'}s%n
    |""".trimMargin("|")
    LogManager.getLogManager().readConfiguration(prop.byteInputStream(Charsets.UTF_8))
    return Logger.getLogger(name)
}
package de.amklee.monomovie.util

import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Suppress("NOTHING_TO_INLINE")
inline fun setupLogging() {
	object {}.javaClass.getResourceAsStream("/logging.properties").use {
		LogManager.getLogManager().readConfiguration(it)
	}
}

class StdoutConsoleHandler : ConsoleHandler() {
	init {
		setOutputStream(System.out)
	}
}

class ColorConsoleFormatter : Formatter() {
	override fun format(record: LogRecord): String = buildString {
		append(TS.format(Instant.fromEpochMilliseconds(record.millis).toJavaInstant()))
		append(" ")
		val lvl = record.level.intValue()
		append(
			when {
				lvl >= Level.SEVERE.intValue() -> RED_BOLD
				lvl >= Level.WARNING.intValue() -> YELLOW
				lvl > Level.INFO.intValue() -> GREEN
				else -> BLUE
			},
		)
		append(record.level.name)
		append(RESET)
		append(" ")
		append(GREEN_BOLD)
		append(record.loggerName ?: "")
		append(RESET)
		append(" ")
		append(formatMessage(record))
		append(System.lineSeparator())

		if (record.thrown != null) {
			val sw = StringWriter()
			PrintWriter(sw).use { pw ->
				record.thrown.printStackTrace(pw)
			}
			append(sw.toString())
		}
	}

	companion object {
		private const val RESET = "\u001b[0m"
		private const val RED_BOLD = "\u001b[1;31m"
		private const val YELLOW = "\u001b[33m"
		private const val GREEN = "\u001b[32m"
		private const val BLUE = "\u001b[34m"
		private const val GREEN_BOLD = "\u001b[1;32m"

		private val TS: DateTimeFormatter =
			DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
	}
}

fun System.Logger.debug(message: () -> String) = this.log(System.Logger.Level.DEBUG, message)

fun System.Logger.info(message: () -> String) = this.log(System.Logger.Level.INFO, message)

fun System.Logger.warn(message: () -> String) = this.log(System.Logger.Level.WARNING, message)

fun System.Logger.error(message: () -> String) = this.log(System.Logger.Level.ERROR, message)

fun System.Logger.error(e: Throwable, message: () -> String) = this.log(System.Logger.Level.ERROR, message, e)

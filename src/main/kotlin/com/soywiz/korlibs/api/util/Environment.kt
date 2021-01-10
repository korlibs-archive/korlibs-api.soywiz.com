package com.soywiz.korlibs.api.util

import java.io.File
import java.util.*

object Environment {
	private val envProperties = File(".env").takeIf { it.exists() }?.let { file -> Properties().also { it.load(file.readText().reader()) } }

	operator fun get(key: String): String? {
		return System.getenv(key) ?: envProperties?.get(key)?.toString()
	}
}
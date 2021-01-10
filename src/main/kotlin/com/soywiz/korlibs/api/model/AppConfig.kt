package com.soywiz.korlibs.api.model

import com.soywiz.korinject.Singleton
import com.soywiz.korlibs.api.util.Environment
import java.io.File

@Singleton
class AppConfig {
	val PORT = Environment["PORT"]?.toIntOrNull() ?: 8080
	val SLACK_TOKEN = Environment["SLACK_TOKEN"]
	val SLACK_VERIFICATION_TOKEN = Environment["SLACK_VERIFICATION_TOKEN"]
	val DISCORD_TOKEN = Environment["DISCORD_TOKEN"]
	val DISCORD_GUILD_ID = 728582275884908604L
	val localDir = File(".").absoluteFile
	val dataDir = File("$localDir/data").also { it.mkdirs() }
	val h2DataFile = File("$dataDir/data.db")

}
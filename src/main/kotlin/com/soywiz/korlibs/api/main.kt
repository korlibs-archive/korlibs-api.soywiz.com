package com.soywiz.korlibs.api

import com.soywiz.korinject.*
import com.soywiz.korlibs.api.bot.*
import com.soywiz.korlibs.api.model.*
import com.soywiz.korlibs.api.route.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*

suspend fun main() {
	val injector = AsyncInjector().jvmAutomapping()

	injector.mapSingleton {
		HttpClient {
			install(JsonFeature) {
				serializer = JacksonSerializer()
			}
		}
	}

	val config = injector.getSync<AppConfig>()

	println("Listening to PORT=${config.PORT}")

	embeddedServer(Netty, port = config.PORT) {
		injector.mapInstance<Application>(this)
		install(DefaultHeaders)
		install(CallLogging)
		install(CORS) {
			method(HttpMethod.Get)
			method(HttpMethod.Post)
			method(HttpMethod.Put)
			method(HttpMethod.Patch)
			method(HttpMethod.Options)
			method(HttpMethod.Delete)
			anyHost()
			allowCredentials = true
		}
		injector.getSync<VersionsRoute>().register()
		injector.getSync<SlackCommandsRoute>().register()
		launch {
			for (bot in listOf(
				injector.get<DiscordRoleUpdaterBot>(),
				injector.get<SlackNotifier>(),
			)) {
				println("Starting BOT... $bot")
				launch {
					bot.run()
				}
			}
			println("Completed")
		}
	}.start(wait = true)
}


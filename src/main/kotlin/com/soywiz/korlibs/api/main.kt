package com.soywiz.korlibs.api

import com.soywiz.kminiorm.*
import com.soywiz.korinject.*
import com.soywiz.korlibs.api.bot.*
import com.soywiz.korlibs.api.model.*
import com.soywiz.korlibs.api.route.*
import com.soywiz.korlibs.api.service.*
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

	val routes = listOf(
		VersionsRoute::class,
		SlackCommandsRoute::class,
	)

	val bots = listOf(
		DiscordRoleUpdaterBot::class,
		SlackNotifier::class,
	)

	val config = injector.getSync<AppConfig>()
	println("DB: ${config.h2DataFile}")
	injector.mapInstance<Db>(JdbcDb("jdbc:h2:${config.h2DataFile}", "", ""))

	embeddedServer(Netty, port = config.PORT) {
		injector.mapInstance<Application>(this)
		injector.mapSingleton {
			HttpClient {
				install(JsonFeature) {
					serializer = JacksonSerializer()
				}
			}
		}

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
		for (clazz in routes) {
			injector.getSync(clazz).register(this)
		}
		//runBlocking {
		//	println("TIERS:")
		//	for (tier in injector.get<Github>().getTiersInfo("soywiz")) {
		//		println("tier: $tier")
		//	}
		//}
		launch {
			for (clazz in bots) {
				val bot = injector.get(clazz)
				println("Starting BOT... $bot")
				launch {
					bot.run()
				}
			}
			println("Completed")
		}
	}.start(wait = true)
}


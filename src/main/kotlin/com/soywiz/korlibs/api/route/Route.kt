package com.soywiz.korlibs.api.route

import io.ktor.application.*
import io.ktor.routing.*
import kotlinx.coroutines.*

interface Route {
	suspend fun Routing.register()
	fun register(application: Application) {
		application.routing { runBlocking { register() } }
	}
}
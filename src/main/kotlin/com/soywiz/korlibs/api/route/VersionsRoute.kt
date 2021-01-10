package com.soywiz.korlibs.api.route

import com.soywiz.korinject.*
import com.soywiz.korlibs.api.service.*
import com.soywiz.korlibs.api.util.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

@Singleton
class VersionsRoute(
	private val application: Application,
	private val bintray: Bintray,
) : Route {
	override suspend fun Routing.register() {
		get("/versions/{id}") {
			val id = context.parameters["id"] ?: ""
			val version = bintray.getLibraryVersion(id)
			call.respond(mapOf("project" to id, "version" to version).toJson())
		}
	}
}
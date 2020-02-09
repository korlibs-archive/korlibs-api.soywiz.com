package com.soywiz.korlibs.api

import com.soywiz.korlibs.api.util.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
	embeddedServer(Netty, port = 8080) {
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
		val httpClient = HttpClient()
		routing {
			get("/versions/{id}") {
				val id = context.parameters["id"].takeIf { (it ?: "").matches(Regex("^[\\w+-]+$")) } ?: error("Invalid id")
				val str = httpClient.get<String>("https://api.bintray.com/packages/korlibs/korlibs/$id?attribute_values=1")
				val json = str.fromJsonUntyped() as Map<String, Any?>
				call.respond(mapOf("project" to id, "version" to json["latest_version"]).toJson())
			}
		}
	}.start(wait = true)
}

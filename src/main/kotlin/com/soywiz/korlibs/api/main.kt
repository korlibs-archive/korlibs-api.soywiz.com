package com.soywiz.korlibs.api

import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korlibs.api.util.*
import io.vertx.core.*
import io.vertx.core.buffer.*
import io.vertx.core.http.*
import io.vertx.core.streams.*
import io.vertx.ext.web.*
import io.vertx.ext.web.handler.*
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.*
import java.net.*
import kotlin.coroutines.*

suspend fun main() {
	val templates = TemplateEngine(File("src/main/resources"))
	val vertx = Vertx.vertx()
	val router = Router.router(vertx)
	val httpClient = HttpClient()
	router.apply {
		//route("/") {
		//templates.renderTemplate("template.ftl", mapOf("model" to MyModel()))
		//}
		router.route().handler(
			CorsHandler.create("*")
				.allowedHeaders(hashSetOf(
					"x-requested-with", "Access-Control-Allow-Origin", "origin",
					"Content-Type", "accept", "X-PINGARUNER"
				))
				.allowedMethods(setOf(
					HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.PUT
				))
		);

		route("/versions/:id") {
			val id = this.pathParam("id").takeIf { it.matches(Regex("^[\\w+-]+$")) } ?: error("Invalid id")
			val json = withContext(Dispatchers.IO) {
				URL("https://api.bintray.com/packages/korlibs/korlibs/$id?attribute_values=1").readText()
			}.fromJsonUntyped() as Map<String, Any?>
			//val str = httpClient.readJson("https://api.bintray.com/packages/korlibs/korlibs/$id?attribute_values=1")

			Json(mapOf("project" to id, "version" to json["latest_version"]))
		}
	}

	println("Listening...8080")
	vertx.createHttpServer().requestHandler(router).listen(8080)
	Unit
}

suspend fun HttpClientResponse.readBytes(): ByteArray {
	val buf = Buffer.buffer()
	this.bodyHandler {
		buf.appendBuffer(it)
	}
	return buf.bytes
}

suspend fun HttpClientRequest.getResponse(vertx: Vertx): HttpClientResponse {
	val channel = toChannel2(vertx)
	val response = channel.receive()
	channel.close()
	return response
}

fun <T> ReadStream<T>.toChannel2(vertx: Vertx): Channel<T> {
	this.pause()
	val ret = ChannelReadStream(
		stream = this,
		channel = Channel(0),
		context = vertx.orCreateContext
	)
	ret.subscribe()
	this.fetch(1)
	return ret.channel
}

private class ChannelReadStream<T>(val stream: ReadStream<T>, val channel: Channel<T>, context: Context) : Channel<T> by channel, CoroutineScope {

	override val coroutineContext: CoroutineContext = context.dispatcher()
	fun subscribe() {
		stream.endHandler {
			close()
		}
		stream.exceptionHandler { err ->
			close(err)
		}
		stream.handler { event ->
			launch {
				send(event)
				stream.fetch(1)
			}
		}
	}
}

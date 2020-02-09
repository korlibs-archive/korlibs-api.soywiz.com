package com.soywiz.korlibs.api.util

/*
import io.vertx.core.http.*
import io.vertx.ext.web.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun Router.route(path: String, method: HttpMethod = HttpMethod.GET, handler: suspend RoutingContext.() -> Any?) {
	route(method, path).handler { routingContext: RoutingContext ->
		handler.startCoroutine(routingContext, object : Continuation<Any?> {
			override val context: CoroutineContext = Dispatchers.Unconfined
			override fun resumeWith(result: Result<Any?>) {
				if (result.isSuccess) {
					val out = result.getOrThrow()
					routingContext.response()
						.putHeader("Content-Type", if (out is IResponse) out.contentType else "text/html")
						.end(out.toString())
				} else {
					val out = result.exceptionOrNull()
					routingContext.response()
						.setStatusCode(500)
						.putHeader("content-type", if (out is IResponse) out.contentType else "text/html")
						.end(out.toString())
				}
			}
		})
	}
}

interface IResponse {
	val contentType: String get() = "text/html"
}

class Json(val obj: Any?) : IResponse {
	val str by lazy { obj.toJson() }
	override val contentType: String = "application/json"
	override fun toString(): String = str
}
*/

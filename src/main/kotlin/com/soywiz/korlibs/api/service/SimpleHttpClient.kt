package com.soywiz.korlibs.api.service

import com.soywiz.korinject.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*

open class SimpleHttpClient(
	private val client: HttpClient
) {
	open suspend fun getString(url: String, headers: List<Pair<String, Any?>> = listOf()): String = client.get<String>(url) {
		for ((k, v) in headers) header(k, v)
	}
	open suspend fun postString(url: String, body: Any, headers: List<Pair<String, Any?>> = listOf()): String = client.post<String>(url) {
		this.body = when (body) {
			is String -> TextContent(body, contentType = ContentType.Any)
			else -> body
		}
		for ((k, v) in headers) header(k, v)
	}
}
package com.soywiz.korlibs.api.service

import com.soywiz.korinject.*
import io.ktor.client.*
import io.ktor.client.request.*

open class SimpleHttpClient(
	private val client: HttpClient
) {
	open suspend fun getString(url: String): String {
		return client.get<String>(url)
	}
}
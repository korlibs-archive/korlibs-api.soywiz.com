package com.soywiz.korlibs.api.service

import com.soywiz.korinject.Singleton
import com.soywiz.korlibs.api.model.AppConfig
import com.soywiz.korlibs.api.model.ProjectVersion
import com.soywiz.korlibs.api.model.Tables
import com.soywiz.korlibs.api.util.fromJsonUntyped
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

@Singleton
class Slack(
	private val httpClient: HttpClient,
	private val config: AppConfig,
	private val tables: Tables,
) {
	suspend fun sendSlackMessage(channel: String, text: String) {
		val result = httpClient.post<String>("https://slack.com/api/chat.postMessage") {
			header("Authorization", "Bearer ${config.SLACK_TOKEN}")
			contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
			this.body = mapOf(
				"channel" to channel,
				"text" to text
			)
		}
		//println("RESULT: $result")
	}
}
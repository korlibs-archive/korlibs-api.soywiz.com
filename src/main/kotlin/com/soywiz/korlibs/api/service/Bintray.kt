package com.soywiz.korlibs.api.service

import com.soywiz.korinject.Singleton
import com.soywiz.korlibs.api.model.AppConfig
import com.soywiz.korlibs.api.model.ProjectVersion
import com.soywiz.korlibs.api.model.Tables
import com.soywiz.korlibs.api.util.fromJsonUntyped
import io.ktor.client.*
import io.ktor.client.request.*

@Singleton
class Bintray(
	private val httpClient: HttpClient,
	private val config: AppConfig,
	private val tables: Tables,
) {
	suspend fun getLibraryVersion(projectId: String): String {
		check(projectId.matches(Regex("^[\\w+-]+$")))
		val str = httpClient.get<String>("https://api.bintray.com/packages/korlibs/korlibs/$projectId?attribute_values=1")
		val json = str.fromJsonUntyped() as Map<String, Any?>
		val version = json["latest_version"].toString()
		tables.projectVersions.upsert(ProjectVersion(projectId, version))
		return version
	}
}
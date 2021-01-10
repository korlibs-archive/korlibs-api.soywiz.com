package com.soywiz.korlibs.api.service

import com.soywiz.korinject.*
import com.soywiz.korlibs.api.model.*
import com.soywiz.korlibs.api.util.*

@Singleton
class Bintray(
	private val httpClient: SimpleHttpClient,
	private val config: AppConfig,
	private val tables: Tables,
	private val time: TimeService,
) {
	fun checkProjectId(projectId: String) {
		check(projectId.matches(Regex("^[\\w+-]+$")))
	}

	suspend fun getLibraryVersion(projectId: String): String {
		checkProjectId(projectId)
		val projectVersion = tables.projectVersions.findOne { it::project eq projectId }
		if (projectVersion != null && projectVersion.isRecent(time)) {
			return projectVersion.version
		}
		return getLibraryVersionUncached(projectId)
	}

	suspend fun getLibraryVersionUncached(projectId: String): String {
		checkProjectId(projectId)
		val str = httpClient.getString("https://api.bintray.com/packages/korlibs/korlibs/$projectId?attribute_values=1")
		val json = str.fromJsonUntyped() as Map<String, Any?>
		val version = json["latest_version"].toString()
		tables.projectVersions.upsert(ProjectVersion(projectId, version, time.now().unixMillisLong))
		return version
	}
}
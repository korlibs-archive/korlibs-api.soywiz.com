package com.soywiz.korlibs.api.model

import com.soywiz.klock.*
import com.soywiz.kminiorm.*
import com.soywiz.korinject.Singleton
import com.soywiz.korlibs.api.service.*
import java.io.File

@Singleton
class Tables(
	config: AppConfig
) {
	val db = JdbcDb("jdbc:h2:${config.h2DataFile}", "", "")
	init {
		println("DB: ${config.h2DataFile}")
	}
	val projectVersions = db.tableBlocking<ProjectVersion>()
	val slackChannelNotifications = db.tableBlocking<SlackChannelNotification>()
}

data class ProjectVersion(
	@DbUnique
	val project: String,
	val version: String,
	val updated: Long,
	override val _id: DbRef<ProjectVersion> = DbRef()
) : DbModel {
}
val ProjectVersion.updatedDateTime get() = DateTime.fromUnix(updated)
fun ProjectVersion.isRecent(time: TimeService) = (time.now() - updatedDateTime) < 15.minutes

data class SlackChannelNotification(
	@DbUnique("project_channel")
	val project: String,
	@DbUnique("project_channel")
	val slackChannel: String,
	val latestPublishedVersion: String,
	override val _id: DbRef<SlackChannelNotification> = DbRef()
) : DbModel

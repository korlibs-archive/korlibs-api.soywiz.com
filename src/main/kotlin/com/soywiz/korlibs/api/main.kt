@file:UseExperimental(ExperimentalTime::class)

package com.soywiz.korlibs.api

import com.soywiz.kminiorm.*
import com.soywiz.korlibs.api.util.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.time.*
import java.io.*
import kotlin.time.*

val httpClient by lazy { HttpClient() {
	install(JsonFeature) {
		serializer = JacksonSerializer()
	}
} }

val PORT by lazy { System.getenv("PORT")?.toIntOrNull() ?: 8080 }
val SLACK_TOKEN by lazy { System.getenv("SLACK_TOKEN") }
val SLACK_VERIFICATION_TOKEN by lazy { System.getenv("SLACK_VERIFICATION_TOKEN") }

data class ProjectVersion(
	@DbUnique
	val project: String,
	val version: String,
	override val _id: DbRef<ProjectVersion> = DbRef()
) : DbModel

data class SlackChannelNotification(
	@DbUnique("project_channel")
	val project: String,
	@DbUnique("project_channel")
	val slackChannel: String,
	val latestPublishedVersion: String,
	override val _id: DbRef<SlackChannelNotification> = DbRef()
) : DbModel

@OptIn(ExperimentalTime::class)
fun main() {
	val localDir = File(".").absoluteFile
	val dataDir = File("$localDir/data").also { it.mkdirs() }
	val h2DataFile = File("$dataDir/data.db")
	println("DB: $h2DataFile")
	val db = JdbcDb("jdbc:h2:$h2DataFile", "", "")

	runBlocking {
		val projectVersions = db.table<ProjectVersion>()
		val slackChannelNotifications = db.table<SlackChannelNotification>()

		//projectVersions.upsert(ProjectVersion("hello", "world"))

		suspend fun getLibraryVersion(projectId: String): String {
			check(projectId.matches(Regex("^[\\w+-]+$")))
			val str = httpClient.get<String>("https://api.bintray.com/packages/korlibs/korlibs/$projectId?attribute_values=1")
			val json = str.fromJsonUntyped() as Map<String, Any?>
			val version = json["latest_version"].toString()
			projectVersions.upsert(ProjectVersion(projectId, version))
			return version
		}

		suspend fun sendSlackMessage(channel: String, text: String) {
			val result = httpClient.post<String>("https://slack.com/api/chat.postMessage") {
				header("Authorization", "Bearer $SLACK_TOKEN")
				contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
				this.body = mapOf(
					"channel" to channel,
					"text" to text
				)
			}
			//println("RESULT: $result")
		}

		//sendSlackMessage("GTVP6G9GE", "HELLO WORLD!")

		launch(Dispatchers.Unconfined) {
			while (true) {
				try {
					println("Started 60-min notifier")
					for ((project, notifs) in slackChannelNotifications.findAll().groupBy { it.project }) {
						try {
							val version = getLibraryVersion(project)
							println("  - Checking $project...$version")
							for (notif in notifs) {
								try {
									if (version != notif.latestPublishedVersion) {
										val msg = "Released `${version}` of `${notif.project}`"
										println("     - $msg")
										slackChannelNotifications.upsert(notif.copy(latestPublishedVersion = version))
										/*
										slackChannelNotifications.update(
											Partial(
												SlackChannelNotification::latestPublishedVersion to version
											)
										) {
											SlackChannelNotification::_id eq notif._id
										}
										*/
										sendSlackMessage(notif.slackChannel, "${notif.slackChannel}: $msg")
									}
								} catch (e: Throwable) {
									e.printStackTrace()
								}
							}
						} catch (e: Throwable) {
							e.printStackTrace()
						}
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}
				delay(60.minutes.toJavaDuration())
			}
		}

		embeddedServer(Netty, port = PORT) {
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
			routing {
				get("/versions/{id}") {
					val id = context.parameters["id"] ?: ""
					val version = getLibraryVersion(id)
					call.respond(mapOf("project" to id, "version" to version).toJson())
				}

				// /slack/command: Parameters [token=[...], team_id=[T03121312], team_domain=[soywiz], channel_id=[GTVP23123213], channel_name=[privategroup], user_id=[U03123213123], user_name=[soywiz], command=[/register_bintray], text=[hello], response_url=[https://hooks.slack.com/commands/21312321/21312312/213123123], trigger_id=[21312312321.2312312.123213123123131231]]

				// https://korlibs-api.soywiz.com/slack/command/
				post("/slack/command/") {
					val params = context.receiveParameters()
					if (params["token"] != SLACK_VERIFICATION_TOKEN) error("Invalid token")
					if (params["user_name"] != "soywiz") error("Invalid user")
					val command = params["command"] ?: ""
					val text = params["text"] ?: ""
					val channelId = params["channel_id"] ?: ""
					val channelName = params["channel_name"] ?: ""
					val response = when (command) {
						"/register_bintray" -> {
							val projectId = text.trim()
							val version = getLibraryVersion(projectId)
							slackChannelNotifications.upsert(SlackChannelNotification(projectId, channelId, version))
							"Registered to '$projectId' (current version '$version') in '$channelName'"
						}
						"/send_test_message" -> {
							//sendSlackMessage("GTVP6G9GE", "HELLO WORLD!")
							val parts = text.trim().split(" ", limit = 2)
							sendSlackMessage(parts[0], parts[1])
							"OK"
						}
						"/slack_info" -> {
							"Channel: '$channelId'"
						}
						else -> {
							"Unknown command $command"
						}
					}
					call.respond(response)
				}
			}
		}.start(wait = true)
	}
}


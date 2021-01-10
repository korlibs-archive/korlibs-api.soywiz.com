package com.soywiz.korlibs.api.route

import com.soywiz.kminiorm.where.*
import com.soywiz.korinject.*
import com.soywiz.korlibs.api.bus.*
import com.soywiz.korlibs.api.model.*
import com.soywiz.korlibs.api.service.*
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

@Singleton
class SlackCommandsRoute(
	private val application: Application,
	private val config: AppConfig,
	private val events: AppEvents,
	private val slack: Slack,
	private val bintray: Bintray,
	private val tables: Tables,
) {
	fun register() {
		application.routing {

			// /slack/command: Parameters [token=[...], team_id=[T03121312], team_domain=[soywiz], channel_id=[GTVP23123213], channel_name=[privategroup], user_id=[U03123213123], user_name=[soywiz], command=[/register_bintray], text=[hello], response_url=[https://hooks.slack.com/commands/21312321/21312312/213123123], trigger_id=[21312312321.2312312.123213123123131231]]

			// https://korlibs-api.soywiz.com/slack/command/
			post("/slack/command/") {
				val params = context.receiveParameters()
				if (params["token"] != config.SLACK_VERIFICATION_TOKEN) error("Invalid token")
				if (params["user_name"] != "soywiz") error("Invalid user")
				val command = params["command"] ?: ""
				val text = params["text"] ?: ""
				val channelId = params["channel_id"] ?: ""
				val channelName = params["channel_name"] ?: ""
				val response = when (command) {
					"/recheck" -> {
						events.triggerRecheck.send(Unit)
						"Triggered recheck"
					}
					"/list_registrations" -> {
						val projectNames = tables.slackChannelNotifications.where { (it::slackChannel eq channelId) }.find().map { it.project }
						"Registered to $projectNames in '$channelName'"
					}
					"/register_bintray" -> {
						val projectId = text.trim()
						val version = bintray.getLibraryVersion(projectId)
						tables.slackChannelNotifications.upsert(SlackChannelNotification(projectId, channelId, version))
						"Registered to '$projectId' (current version '$version') in '$channelName'"
					}
					"/unregister_bintray" -> {
						val projectId = text.trim()
						val version = bintray.getLibraryVersion(projectId)
						tables.slackChannelNotifications
							.where { (it::project eq projectId) AND (it::slackChannel eq channelId) AND (it::latestPublishedVersion eq version) }
							.delete()
						"Unregistered from '$projectId' (current version '$version') in '$channelName'"
					}
					"/send_test_message" -> {
						//sendSlackMessage("GTVP6G9GE", "HELLO WORLD!")
						val parts = text.trim().split(" ", limit = 2)
						slack.sendSlackMessage(parts[0], parts[1])
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
	}
}
package com.soywiz.korlibs.api.bot

import com.soywiz.klock.*
import com.soywiz.korinject.*
import com.soywiz.korlibs.api.model.*
import com.soywiz.korlibs.api.service.*
import com.soywiz.korio.async.*
import com.soywiz.korlibs.api.bus.*
import kotlinx.coroutines.*

@Singleton
class SlackNotifier(
	private val slack: Slack,
	private val bintray: Bintray,
	private val tables: Tables,
	private val events: AppEvents,
) : Bot {
	override suspend fun run() {
		while (true) {
			try {
				println("Started 60-min notifier")
				for ((project, notifs) in tables.slackChannelNotifications.findAll().groupBy { it.project }) {
					try {
						val version = bintray.getLibraryVersion(project)
						println("  - Checking $project...$version")
						for (notif in notifs) {
							try {
								if (version != notif.latestPublishedVersion) {
									val msg = "Released `${version}` of `${notif.project}`"
									println("     - $msg")
									tables.slackChannelNotifications.upsert(notif.copy(latestPublishedVersion = version))
									/*
									slackChannelNotifications.update(
										Partial(
											SlackChannelNotification::latestPublishedVersion to version
										)
									) {
										SlackChannelNotification::_id eq notif._id
									}
									*/
									slack.sendSlackMessage(notif.slackChannel, "${notif.slackChannel}: $msg")
									delay(1.seconds)
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
			try {
				withTimeout(60.minutes) {
					events.triggerRecheck.receive()
				}
			} catch (e: TimeoutCancellationException) {
				Unit
			} catch (e: Throwable) {
				e.printStackTrace()
			}
			//delay(60.minutes.toJavaDuration())
		}
	}
}
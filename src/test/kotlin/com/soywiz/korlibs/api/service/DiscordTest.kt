package com.soywiz.korlibs.api.service

import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import kotlin.test.*

class DiscordTest : BaseServiceTest<Discord>(Discord::class) {
	@Test
	fun test() = suspendTest {
		/*
		for (guild in service.getGuilds()) {
			println("guild: ${guild.id.asString} ${guild.name}")
		}

		for (role in service.getRoles()) {
			println("guild: ${role.key}, ${role.value}")
		}
		 */
	}
}
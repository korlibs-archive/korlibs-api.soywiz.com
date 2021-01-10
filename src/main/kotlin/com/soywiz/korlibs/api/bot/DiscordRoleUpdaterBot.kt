package com.soywiz.korlibs.api.bot

import com.soywiz.korinject.Singleton
import com.soywiz.korlibs.api.model.AppConfig
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.*
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList

@Singleton
class DiscordRoleUpdaterBot(
	private val config: AppConfig
) : Bot {
	override suspend fun run() {
		val client = Kord(config.DISCORD_TOKEN ?: error("Can't find environment DISCORD_TOKEN"))
		val pingPong = ReactionEmoji.Unicode("\uD83C\uDFD3")

		val guilds = client.guilds.toList()
		val korgeGuild = guilds.first { it.id == Snowflake(config.DISCORD_GUILD_ID) }
		//println("guilds: ${guilds.size}")
		//for (guild in guilds) println("guild: $guild")
		val roles = korgeGuild.roles.toList()
		val kingRole = roles.firstOrNull { it.name == "King" }
		val highCouncilRole = roles.firstOrNull { it.name == "High Council" }
		val ministerRole = roles.firstOrNull { it.name == "Minister" }
		val mageRole = roles.firstOrNull { it.name == "Mage" }
		val knightRole = roles.firstOrNull { it.name == "Knight" }

		//println(client.guilds.toList())
		client.on<MessageCreateEvent> {
			if (message.content != "!ping") return@on

			//val response = message.channel.createMessage("Pong! : ${knightRole!!.id}")
			//response.addReaction(pingPong)

			//message.author!!.asMemberOrNull(korgeGuild.id)!!.addRole(demoRole!!.id, "yay!")

			// java.lang.ClassCastException: dev.kord.core.entity.channel.DmChannel cannot be cast to dev.kord.core.entity.channel.GuildChannel
			//message.getAuthorAsMember()!!.addRole(demoRole!!.id, "yay!")

			//delay(5000L)
			//message.delete()
			//response.delete()
		}

		client.login()
	}
}
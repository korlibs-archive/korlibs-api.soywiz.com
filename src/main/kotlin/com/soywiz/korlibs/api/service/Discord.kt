package com.soywiz.korlibs.api.service

import com.soywiz.korinject.*
import com.soywiz.korlibs.api.model.*
import dev.kord.common.entity.*
import dev.kord.rest.service.*

@Singleton
open class Discord(
	private val config: AppConfig,
) {
	open val rest = RestClient(config.DISCORD_TOKEN)
	open val defaultGuildId get() = config.DISCORD_GUILD_ID.toString()

	open suspend fun getGuilds(): List<DiscordPartialGuild> =
		rest.user.getCurrentUserGuilds()

	open suspend fun getRoles(guildId: String = defaultGuildId): Map<String, Snowflake> =
		rest.guild.getGuildRoles(Snowflake(guildId)).associate { it.name to it.id }

	open suspend fun addRole(userId: String, role: DiscordRole, guildId: String = defaultGuildId, reason: String? = null) {
		rest.guild.addRoleToGuildMember(Snowflake(guildId), Snowflake(userId), role.id, reason)
	}

	open suspend fun removeRole(userId: String, role: DiscordRole, guildId: String = defaultGuildId, reason: String? = null) {
		rest.guild.deleteRoleFromGuildMember(Snowflake(guildId), Snowflake(userId), role.id, reason)
	}
}
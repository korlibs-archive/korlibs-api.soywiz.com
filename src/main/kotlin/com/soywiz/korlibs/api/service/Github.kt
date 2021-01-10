package com.soywiz.korlibs.api.service

import com.fasterxml.jackson.module.kotlin.*
import com.soywiz.klock.*
import com.soywiz.korinject.*
import com.soywiz.korio.lang.*
import com.soywiz.korlibs.api.util.*
import com.soywiz.korlibs.api.util.Environment
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.net.*

@Singleton
open class Github(
	val config: Config,
	val time: TimeService,
	val httpClient: SimpleHttpClient,
) {
	@Singleton
	open class Config {
		open val GH_CLIENT_ID get() = Environment.getSure("GH_CLIENT_ID")
		open val GH_CLIENT_SECRET get() = Environment.getSure("GH_CLIENT_SECRET")
		open val GH_SPONSOR_TOKEN get() = Environment.getSure("GH_SPONSOR_TOKEN")
	}

	suspend fun oauthGetAccessToken(code: String, clientId: String, clientSecret: String): String {
		val result = httpClient.postString("https://github.com/login/oauth/access_token", FormDataContent(Parameters.build {
			append("client_id", clientId)
			append("client_secret", clientSecret)
			append("code", code)
		}))
		val params = result.parseUrlEncodedParameters()
		return params["access_token"] ?: error("Can't get access token")
	}

	suspend fun oauthGetUserLogin(access_token: String): String {
		val result = httpClient.getString("https://api.github.com/user", headers = listOf(
			"Authorization" to "token $access_token"
		))
		val data = jsonMapper.readValue<Map<String, Any?>>(result)
		return data["login"].toString()
	}

	val jsonMapper = jacksonObjectMapper()

	open suspend fun graphqlCallString(access_token: String, query: String): String {
		return httpClient.postString(
			"https://api.github.com/graphql",
			jsonMapper.writeValueAsString(mapOf("query" to query)),
			headers = listOf("Authorization" to "bearer $access_token")
		)
	}

	open suspend fun graphqlCall(access_token: String, query: String): Map<String, Any?> {
		//println("result: $result")
		return jsonMapper.readValue<Map<String, Any?>>(graphqlCallString(access_token, query))
	}

	object Dynamic {
		inline operator fun <T> invoke(block: Dynamic.() -> T): T = block()

		fun Any?.keys(): List<Any?> = when (this) {
			is Map<*, *> -> this.keys.toList()
			is Iterable<*> -> (0 until this.count()).toList()
			else -> listOf()
		}

		fun Any?.entries(): List<Pair<Any?, Any?>> = keys().map { it to this[it] }

		operator fun Any?.get(key: Any?): Any? = when (this) {
			is Map<*, *> -> (this as Map<Any?, Any?>)[key]
			else -> null
		}

		val Any?.str get() = this.toString()
		val Any?.int get() = when (this) {
			is Number -> this.toInt()
			else -> this.toString().toIntOrNull() ?: 0
		}
		val Any?.list: List<Any?> get() = when (this) {
			is Iterable<*> -> this.toList()
			null -> listOf()
			else -> listOf(this)
		}
	}

	suspend fun getUserLogin(access_token: String = config.GH_SPONSOR_TOKEN): String {
		val data = graphqlCall(
			access_token,
			"query { viewer { login } }"
		)
		return Dynamic { data["data"]["viewer"]["login"].str }
	}

	data class SponsorInfo(val login: String, val price: Int, val date: DateTime)

	data class TierInfo(val id: String, val name: String, val description: String, val monthlyPriceInDollars: Int)

	suspend fun getTierInfo(login: String, access_token: String = config.GH_SPONSOR_TOKEN): List<TierInfo> {
		val data = graphqlCall(
			access_token, """
				query {
				  user(login: ${login.quoted}) {
					sponsorsListing {
					  tiers(last: 100) {
						nodes {
						  id
						  name
						  description
						  monthlyPriceInDollars
						}
					  }
					}
				  }
				}
			""".trimIndent()
		)
		return Dynamic {
			data["data"]["user"]["sponsorsListing"]["tiers"]["nodes"].list.map {
				TierInfo(it["id"].str, it["name"].str, it["description"].str, it["monthlyPriceInDollars"].int)
			}
		}
	}

	suspend fun getSponsorInfo(login: String, access_token: String = config.GH_SPONSOR_TOKEN, otherLogin: String = "soywiz"): SponsorInfo {
		val data = graphqlCall(
			access_token, """
				query { 
				  user(login: ${login.quoted}) {
					sponsorshipsAsSponsor(first: 100) {
					  edges {
						node {
						  sponsorable {
							sponsorsListing {
							  slug
							}
						  }
						  tier {
						  	id
							monthlyPriceInDollars
						  }
						}
					  }
					}
				  }
				}
			""".trimIndent()
		)

		var sponsorPrice: Int = 0
		Dynamic {
			for (edge in data["data"]["user"]["sponsorshipsAsSponsor"]["edges"].list) {
				val slug = edge["node"]["sponsorable"]["sponsorsListing"]["slug"].str
				val price = edge["node"]["tier"]["monthlyPriceInDollars"].int
				if (slug == "sponsors-$otherLogin") {
					sponsorPrice = price
				}
			}
		}
		return SponsorInfo(
			login,
			sponsorPrice,
			time.now()
		)
	}
}
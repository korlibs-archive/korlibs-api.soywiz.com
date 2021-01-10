package com.soywiz.korlibs.api.service

import com.soywiz.klock.*
import com.soywiz.kminiorm.*
import com.soywiz.kminiorm.memory.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import io.mockk.*
import kotlin.test.*

class GithubTest {
	val RETURN_TIME = DateTime.EPOCH
	val injector = AsyncInjector().jvmAutomapping().apply {
		mapInstance<Db>(MemoryDb())
		mapInstance<TimeService>(object : TimeService() {
			override fun now(): DateTime = RETURN_TIME
		})
		mapInstance<Github.Config>(object : Github.Config() {
			override val GH_CLIENT_ID: String = "ghclientid"
			override val GH_CLIENT_SECRET: String = "ghclientsecret"
			override val GH_SPONSOR_TOKEN: String = "ghsponsortoken"
		})
	}
	val github get() = injector.getSync<Github>()

	@Test
	fun testGetUserLogin() = suspendTest {
		injector.mapInstance(mockk<SimpleHttpClient>().also { httpClient ->
			coEvery { httpClient.postString(
				"https://api.github.com/graphql",
				"{\"query\":\"query { viewer { login } }\"}",
				listOf("Authorization" to "bearer ghsponsortoken"))
			} returns """{
			  "data": {
				"viewer": {
				  "login": "demodemo"
				}
			  }
			}"""
		})
		assertEquals("demodemo", github.getUserLogin())
	}

	@Test
	fun testGetSponsorInfo() = suspendTest {
		injector.mapInstance(mockk<SimpleHttpClient>().also { httpClient ->
			coEvery { httpClient.postString(
				"https://api.github.com/graphql",
				any(),
				listOf("Authorization" to "bearer ghsponsortoken"))
			} returns """
				{
				  "data": {
					"user": {
					  "sponsorshipsAsSponsor": {
						"edges": [
						  {
							"node": {
							  "sponsorable": {
								"sponsorsListing": {
								  "slug": "sponsors-soywiz"
								}
							  },
							  "tier": {
								"id": "MDEyOlNwb25zb3JzVGllcjE3NTU0",
								"monthlyPriceInDollars": 15
							  }
							}
						  }
						]
					  }
					}
				  }
				}
			"""
		})
		assertEquals(Github.SponsorInfo("demo", 15, RETURN_TIME), github.getSponsorInfo("demo"))
	}

	@Test
	fun testGetTierInfo() = suspendTest {
		injector.mapInstance(mockk<SimpleHttpClient>().also { httpClient ->
			coEvery { httpClient.postString(
				"https://api.github.com/graphql",
				any(),
				listOf("Authorization" to "bearer ghsponsortoken"))
			} returns """
				{
				  "data": {
					"user": {
					  "sponsorsListing": {
						"tiers": {
						  "nodes": [
							{
							  "id": "MDEyOlNwb25zb3JzVGllcjUzODg=",
							  "name": "name1",
							  "description": "desc1",
							  "monthlyPriceInDollars": 5
							},
							{
							  "id": "MDEyOlNwb25zb3JzVGllcjgyNjY=",
							  "name": "name2",
							  "description": "desc2",
							  "monthlyPriceInDollars": 10
							}
						  ]
						}
					  }
					}
				  }
				}
			"""
		})
		assertEquals(listOf(
			Github.TierInfo(
				id = "MDEyOlNwb25zb3JzVGllcjUzODg=",
				name = "name1",
				description = "desc1",
				monthlyPriceInDollars = 5
			),
			Github.TierInfo(
				id = "MDEyOlNwb25zb3JzVGllcjgyNjY=",
				name = "name2",
				description = "desc2",
				monthlyPriceInDollars = 10
			)
		), github.getTiersInfo("demo"))
	}
}
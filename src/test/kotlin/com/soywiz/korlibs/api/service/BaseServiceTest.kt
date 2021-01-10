package com.soywiz.korlibs.api.service

import com.soywiz.korinject.*
import kotlinx.coroutines.*
import kotlin.reflect.*

abstract class BaseServiceTest<T : Any>(val clazz: KClass<T>) {
	val injector = AsyncInjector().jvmAutomapping()
	val service by lazy { runBlocking { injector.get(clazz) } }
}
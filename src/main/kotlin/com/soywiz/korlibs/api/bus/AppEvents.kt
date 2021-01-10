package com.soywiz.korlibs.api.bus

import com.soywiz.korinject.*
import kotlinx.coroutines.channels.*

@Singleton
class AppEvents {
	val triggerRecheck = Channel<Unit>()
}
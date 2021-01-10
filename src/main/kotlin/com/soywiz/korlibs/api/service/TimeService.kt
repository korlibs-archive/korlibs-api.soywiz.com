package com.soywiz.korlibs.api.service

import com.soywiz.klock.*
import com.soywiz.korinject.*

@Singleton
open class TimeService {
	open fun now() = DateTime.now()
}
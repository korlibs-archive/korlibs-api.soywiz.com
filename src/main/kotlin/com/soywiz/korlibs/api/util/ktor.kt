package com.soywiz.korlibs.api.util

import io.ktor.application.*
import io.ktor.util.pipeline.*

val PipelineContext<*, ApplicationCall>.request get() = context.request

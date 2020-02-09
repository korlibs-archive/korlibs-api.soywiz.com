package com.soywiz.korlibs.api.util

import com.fasterxml.jackson.module.kotlin.*

val JSON = jacksonObjectMapper()

fun Any?.toJson(): String = JSON.writeValueAsString(this)
fun String.fromJsonUntyped(): Any? = JSON.readValue(this)
inline fun <reified T> String.fromJson(): T = JSON.readValue(this, T::class.java)
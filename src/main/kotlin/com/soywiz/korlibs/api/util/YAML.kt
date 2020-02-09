package com.soywiz.korlibs.api.util

import org.yaml.snakeyaml.*

@PublishedApi
internal val globalYaml by lazy { Yaml() }

inline fun <reified T> String.fromYaml(): T = globalYaml.loadAs(this, T::class.java)
fun String.fromYamlUntyped(): Any? = globalYaml.load(this)
fun Any?.toYaml(): String = globalYaml.dump(this)

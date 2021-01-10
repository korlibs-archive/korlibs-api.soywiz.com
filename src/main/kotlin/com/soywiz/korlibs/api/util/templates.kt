package com.soywiz.korlibs.api

import freemarker.cache.*
import freemarker.template.*
import org.intellij.lang.annotations.*
import java.io.*

class TemplateEngine(
	val loader: TemplateLoader
) {
	constructor(basePath: File) : this(FileTemplateLoader(basePath))

	val config = Configuration(Configuration.VERSION_2_3_29).also {
		it.templateLoader = loader
	}

	fun renderTemplate(
		@Language("file-reference")
		template: String,
		model: Any?
	): String {
		val tpl = config.getTemplate(template)
		return StringWriter().also { out ->
			tpl.process(model, out)
		}.toString()
	}
}
/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvcview.mvcviewscriptintegrate

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun configureViewResolvers(registry: ViewResolverRegistry) {
		registry.scriptTemplate()
	}

	@Bean
	fun configurer() = ScriptTemplateConfigurer().apply {
		engineName = "jython"
		setScripts("render.py")
		renderFunction = "render"
	}
}
// end::snippet[]

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvcfnrunning

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.function.RouterFunction

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	@Bean
	fun routerFunctionA(): RouterFunction<*> {
		TODO()
	}

	@Bean
	fun routerFunctionB(): RouterFunction<*> {
		TODO()
	}

	override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
		TODO()
	}

	override fun addCorsMappings(registry: CorsRegistry) {
		TODO()
	}

	override fun configureViewResolvers(registry: ViewResolverRegistry) {
		TODO()
	}
}
// end::snippet[]


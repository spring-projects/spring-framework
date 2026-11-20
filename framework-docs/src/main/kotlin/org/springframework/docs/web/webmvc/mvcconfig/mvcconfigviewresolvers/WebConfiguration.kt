/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

@file:Suppress("DEPRECATION")

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigviewresolvers

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.view.json.JacksonJsonView

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {
	override fun configureViewResolvers(registry: ViewResolverRegistry) {
		registry.enableContentNegotiation(JacksonJsonView())
		registry.jsp()
	}
}
// end::snippet[]

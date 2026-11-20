/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigstaticresources

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.VersionResourceResolver

// tag::snippet[]
@Configuration
class VersionedConfiguration : WebMvcConfigurer {

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry.addResourceHandler("/resources/**")
			.addResourceLocations("/public/")
			.resourceChain(true)
			.addResolver(VersionResourceResolver().addContentVersionStrategy("/**"))
	}
}
// end::snippet[]
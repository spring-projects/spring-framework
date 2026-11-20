/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigstaticresources

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry.addResourceHandler("/resources/**")
			.addResourceLocations("/public", "classpath:/static/")
			.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)))
	}
}
// end::snippet[]

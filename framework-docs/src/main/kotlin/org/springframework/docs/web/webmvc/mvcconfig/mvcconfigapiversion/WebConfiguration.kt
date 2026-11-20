/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigapiversion

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
		configurer.useRequestHeader("API-Version")
	}
}
// end::snippet[]
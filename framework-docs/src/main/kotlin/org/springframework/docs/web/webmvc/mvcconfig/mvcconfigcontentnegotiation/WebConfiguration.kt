/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigcontentnegotiation

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// tag::snippet[]
@Configuration
class WebConfiguration : WebMvcConfigurer {

	override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
		configurer.mediaType("json", MediaType.APPLICATION_JSON)
		configurer.mediaType("xml", MediaType.APPLICATION_XML)
	}
}
// end::snippet[]
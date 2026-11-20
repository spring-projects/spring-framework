/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvcview.mvcviewsfreemarker

import freemarker.template.utility.XmlEscape
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer

@Configuration
class WebConfiguration {

	// tag::snippet[]
	@Bean
	fun freeMarkerConfigurer() = FreeMarkerConfigurer().apply {
		setTemplateLoaderPath("/WEB-INF/freemarker")
		setFreemarkerVariables(mapOf("xml_escape" to XmlEscape()))
	}
	// end::snippet[]
}

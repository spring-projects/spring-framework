/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigconversion

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


// tag::snippet[]
@Configuration
class DateTimeWebConfiguration : WebMvcConfigurer {

	override fun addFormatters(registry: FormatterRegistry) {
		DateTimeFormatterRegistrar().apply {
			setUseIsoFormat(true)
			registerFormatters(registry)
		}
	}
}
// end::snippet[]
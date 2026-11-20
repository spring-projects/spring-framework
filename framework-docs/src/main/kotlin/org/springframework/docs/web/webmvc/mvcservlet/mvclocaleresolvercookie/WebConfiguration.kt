/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvclocaleresolvercookie

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import java.time.Duration

// tag::snippet[]
@Configuration
class WebConfiguration {

	@Bean
	fun localeResolver(): LocaleResolver = CookieLocaleResolver("clientlanguage").apply {
		setCookieMaxAge(Duration.ofSeconds(100000))
	}
}
// end::snippet[]


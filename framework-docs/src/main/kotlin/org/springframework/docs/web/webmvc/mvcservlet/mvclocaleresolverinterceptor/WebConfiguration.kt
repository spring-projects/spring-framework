/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvclocaleresolverinterceptor

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor

// tag::snippet[]
@Configuration
class WebConfiguration {

	@Bean
	fun localeResolver(): LocaleResolver {
		return CookieLocaleResolver()
	}

	@Bean
	fun urlMapping() = SimpleUrlHandlerMapping().apply {
		setInterceptors(LocaleChangeInterceptor().apply {
			paramName = "siteLanguage"
		})
		urlMap = mapOf("/**/*.view" to "someController")
	}
}
// end::snippet[]


/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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


/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvclocaleresolvercookie;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

// tag::snippet[]
@Configuration
public class WebConfiguration {

	@Bean
	public LocaleResolver localeResolver() {
		CookieLocaleResolver localeResolver = new CookieLocaleResolver("clientlanguage");
		localeResolver.setCookieMaxAge(Duration.ofSeconds(100000));
		return localeResolver;
	}
}
// end::snippet[]


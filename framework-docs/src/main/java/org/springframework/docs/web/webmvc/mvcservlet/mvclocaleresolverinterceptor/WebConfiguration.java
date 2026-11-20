/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvclocaleresolverinterceptor;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

// tag::snippet[]
@Configuration
public class WebConfiguration {

	@Bean
	public LocaleResolver localeResolver() {
		return new CookieLocaleResolver();
	}

	@Bean
	public SimpleUrlHandlerMapping urlMapping() {
		SimpleUrlHandlerMapping urlHandlerMapping = new SimpleUrlHandlerMapping();
		LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
		interceptor.setParamName("siteLanguage");
		urlHandlerMapping.setInterceptors(interceptor);
		urlHandlerMapping.setUrlMap(Map.of("/**/*.view", "someController"));
		return urlHandlerMapping;
	}
}
// end::snippet[]


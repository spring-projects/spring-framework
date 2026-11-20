/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvcfnrunning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RouterFunction;

// tag::snippet[]
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Bean
	public RouterFunction<?> routerFunctionA() {
		// ...
		return null;
	}

	@Bean
	public RouterFunction<?> routerFunctionB() {
		// ...
		return null;
	}

	@Override
	public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
		// configure message conversion...
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// configure CORS...
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		// configure view resolution for HTML rendering...
	}
}
// end::snippet[]


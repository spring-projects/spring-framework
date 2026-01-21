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


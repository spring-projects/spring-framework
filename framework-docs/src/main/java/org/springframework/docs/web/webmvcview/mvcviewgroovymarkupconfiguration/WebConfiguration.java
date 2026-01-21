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

package org.springframework.docs.web.webmvcview.mvcviewgroovymarkupconfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;

// tag::snippet[]
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.groovy();
	}

	// Configure the Groovy Markup Template Engine...

	@Bean
	public GroovyMarkupConfigurer groovyMarkupConfigurer() {
		GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
		configurer.setResourceLoaderPath("/WEB-INF/");
		return configurer;
	}
}
// end::snippet[]

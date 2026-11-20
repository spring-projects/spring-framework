/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.httpserver.reactive;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

@Configuration(proxyBeanMethods = false)
public class HttpHandlerConfiguration {

	private final ApplicationContext applicationContext;

	public HttpHandlerConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	public HttpHandler httpHandler(ObservationRegistry registry) {
		return WebHttpHandlerBuilder.applicationContext(this.applicationContext)
				.observationRegistry(registry)
				.build();
	}
}

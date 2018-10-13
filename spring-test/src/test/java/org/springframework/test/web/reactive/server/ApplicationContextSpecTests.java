/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Unit tests with {@link ApplicationContextSpec}.
 * @author Rossen Stoyanchev
 */
public class ApplicationContextSpecTests {


	@Test // SPR-17094
	public void sessionManagerBean() {
		ApplicationContext context = new AnnotationConfigApplicationContext(WebConfig.class);
		ApplicationContextSpec spec = new ApplicationContextSpec(context);
		WebTestClient testClient = spec.configureClient().build();

		for (int i=0; i < 2; i++) {
			testClient.get().uri("/sessionClassName")
					.exchange()
					.expectStatus().isOk()
					.expectBody(String.class).isEqualTo("MockWebSession");
		}
	}


	@Configuration
	@EnableWebFlux
	static class WebConfig {

		@Bean
		public RouterFunction<?> handler() {
			return RouterFunctions.route()
					.GET("/sessionClassName", request ->
							request.session().flatMap(session -> {
								String className = session.getClass().getSimpleName();
								return ServerResponse.ok().syncBody(className);
							}))
					.build();
		}

		@Bean
		public WebSessionManager webSessionManager() {
			MockWebSession session = new MockWebSession();
			return exchange -> Mono.just(session);
		}
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import org.junit.jupiter.api.Test;
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
class ApplicationContextSpecTests {

	@Test // SPR-17094
	void sessionManagerBean() {
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
								return ServerResponse.ok().bodyValue(className);
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

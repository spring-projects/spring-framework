/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.adapter;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that the {@link OpenTelemetryServerRequestObservationConvention}
 * is discovered and applied through the regular {@link WebHttpHandlerBuilder} to
 * {@link HttpWebHandlerAdapter} wiring when handling a real WebFlux request.
 *
 * @author Tommy Ludwig
 * @see OpenTelemetryServerRequestObservationConvention
 * @see HttpWebHandlerAdapter
 */
class OpenTelemetryServerRequestObservationConventionIntegrationTests {

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();


	@Test
	void shouldUseOpenTelemetryConventionForServerRequestObservations() {
		HttpHandler httpHandler = WebHttpHandlerBuilder.webHandler(exchange -> {
			// A HandlerMapping would have matched /users/{id} and recorded the route
			// pattern on the current observation context; the matched handler then runs.
			ServerRequestObservationContext.findCurrent(exchange.getAttributes())
					.ifPresent(context -> context.setPathPattern("/users/{id}"));
			exchange.getResponse().setStatusCode(HttpStatus.OK);
			return Mono.empty();
		})
				.observationRegistry(this.observationRegistry)
				.observationConvention(new OpenTelemetryServerRequestObservationConvention())
				.build();

		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/users/123?foo=bar").build();
		MockServerHttpResponse response = new MockServerHttpResponse();

		httpHandler.handle(request, response).block();

		assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.server.request.duration")
				.that()
				.hasBeenStopped()
				.hasLowCardinalityKeyValue("http.request.method", "GET")
				.hasLowCardinalityKeyValue("http.route", "/users/{id}")
				.hasLowCardinalityKeyValue("url.scheme", "http")
				.hasLowCardinalityKeyValue("http.response.status_code", "200")
				.hasLowCardinalityKeyValue("outcome", "SUCCESS")
				.hasLowCardinalityKeyValue("error.type", "none")
				.hasHighCardinalityKeyValue("url.path", "/users/123")
				.hasHighCardinalityKeyValue("http.request.method_original", "GET");
	}

}

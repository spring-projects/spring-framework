/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.ReactiveReturnTypeTests}.
 *
 * @author Rossen Stoyanchev
 */
class ReactiveReturnTypeTests {

	@Test
	void sseWithFlux() {

		WebTestClient testClient =
				MockMvcWebTestClient.bindToController(new ReactiveController()).build();

		Flux<String> bodyFlux = testClient.get().uri("/spr16869")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
				.returnResult(String.class)
				.getResponseBody();

		StepVerifier.create(bodyFlux)
				.expectNext("event0")
				.expectNext("event1")
				.expectNext("event2")
				.verifyComplete();
	}


	@RestController
	static class ReactiveController {

		@GetMapping(path = "/spr16869", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
		Flux<String> sseFlux() {
			return Flux.interval(Duration.ofSeconds(1)).take(3)
					.map(aLong -> String.format("event%d", aLong));
		}
	}

}

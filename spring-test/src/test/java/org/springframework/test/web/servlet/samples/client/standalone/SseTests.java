/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SSE controller tests with MockMvc and WebTestClient.
 *
 * @author Rossen Stoyanchev
 */
class SseTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new SseController()).build();


	@Test
	void sse() {
		FluxExchangeResult<Person> exchangeResult = this.testClient.get()
				.uri("/persons")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType("text/event-stream")
				.returnResult(Person.class);

		StepVerifier.create(exchangeResult.getResponseBody())
				.expectNext(new Person("N0"), new Person("N1"), new Person("N2"))
				.expectNextCount(4)
				.consumeNextWith(person -> assertThat(person.getName()).endsWith("7"))
				.thenCancel()
				.verify();
	}


	@RestController
	private static class SseController {

		@GetMapping(path = "/persons", produces = "text/event-stream")
		public Flux<Person> getPersonStream() {
			return Flux.interval(ofMillis(100)).take(50).onBackpressureBuffer(50)
					.map(index -> new Person("N" + index));
		}
	}

}

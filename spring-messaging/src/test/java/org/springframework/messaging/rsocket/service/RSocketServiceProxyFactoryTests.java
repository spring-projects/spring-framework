/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.time.Duration;
import java.util.Optional;

import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.publisher.Mono;

import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.TestRSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 * Tests for {@link RSocketServiceProxyFactory}.
 *
 * @author Sam Brannen
 * @since 7.1
 */
class RSocketServiceProxyFactoryTests {

	private final TestRSocket rsocket = new TestRSocket();

	private final RSocketRequester requester =
			RSocketRequester.wrap(this.rsocket, TEXT_PLAIN, TEXT_PLAIN, RSocketStrategies.create());


	@Test  // gh-37451
	@Timeout(5)
	void blockTimeoutZeroSpecifiesInfiniteTimeout() {
		RSocketServiceProxyFactory proxyFactory =
				RSocketServiceProxyFactory.builder(this.requester).blockTimeout(Duration.ZERO).build();
		BlockingService service = proxyFactory.createClient(BlockingService.class);

		this.rsocket.setPayloadMonoToReturn(delayedPayload("p2"));
		assertThat(service.requestResponse("p1")).isEqualTo("p2");

		this.rsocket.setPayloadMonoToReturn(delayedPayload("p3"));
		assertThat(service.requestResponseOptional("p1")).contains("p3");
	}

	@Test  // gh-37451
	void blockTimeoutExceeded() {
		RSocketServiceProxyFactory proxyFactory = RSocketServiceProxyFactory.builder(this.requester)
				.blockTimeout(Duration.ofMillis(10)).build();
		BlockingService service = proxyFactory.createClient(BlockingService.class);

		this.rsocket.setPayloadMonoToReturn(Mono.never());
		assertThatIllegalStateException()
				.isThrownBy(() -> service.requestResponse("p1"))
				.withMessageStartingWith("Timeout on blocking read");
	}

	@Test  // gh-37451
	void negativeBlockTimeoutIsRejected() {
		RSocketServiceProxyFactory.Builder builder = RSocketServiceProxyFactory.builder(this.requester);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> builder.blockTimeout(Duration.ofMillis(-1)))
				.withMessage("Timeout must be a non-negative value");
	}


	private static Mono<io.rsocket.Payload> delayedPayload(String data) {
		return Mono.just(DefaultPayload.create(data)).delayElement(Duration.ofMillis(50));
	}


	private interface BlockingService {

		@RSocketExchange("rr")
		String requestResponse(@Payload String input);

		@RSocketExchange("rr")
		Optional<String> requestResponseOptional(@Payload String input);

	}

}

/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.service.invoker;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractReactorHttpExchangeAdapter}.
 *
 * @author Sam Brannen
 * @since 7.1
 */
class AbstractReactorHttpExchangeAdapterTests {

	private final TestAdapter adapter = new TestAdapter();


	@Test  // gh-37425
	void blockTimeoutZeroSpecifiesInfiniteTimeout() {
		this.adapter.setBlockTimeout(Duration.ZERO);
		assertThat(this.adapter.getBlockTimeout()).isNull();
	}

	@Test  // gh-37425
	void negativeBlockTimeoutIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.adapter.setBlockTimeout(Duration.ofMillis(-1)))
				.withMessage("Timeout must be a non-negative value");
	}


	private static class TestAdapter extends AbstractReactorHttpExchangeAdapter {

		@Override
		public boolean supportsRequestAttributes() {
			return false;
		}

		@Override
		public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
			return Mono.empty();
		}

		@Override
		public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
			return Mono.empty();
		}

		@Override
		public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
			return Mono.empty();
		}

		@Override
		public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
			return Flux.empty();
		}

		@Override
		public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
			return Mono.empty();
		}

		@Override
		public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
				HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

			return Mono.empty();
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
				HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

			return Mono.empty();
		}
	}

}

/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.time.Duration;
import java.util.Collections;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for the {@link WebClient} {@link io.micrometer.observation.Observation observations}.
 * @author Brian Clozel
 */
class WebClientObservationTests {


	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final ExchangeFunction exchangeFunction = mock();

	private final ArgumentCaptor<ClientRequest> request = ArgumentCaptor.forClass(ClientRequest.class);

	private WebClient.Builder builder;

	@BeforeEach
	void setup() {
		ClientResponse mockResponse = mock();
		when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);
		when(mockResponse.bodyToMono(Void.class)).thenReturn(Mono.empty());
		given(this.exchangeFunction.exchange(this.request.capture())).willReturn(Mono.just(mockResponse));
		this.builder = WebClient.builder().baseUrl("/base").exchangeFunction(this.exchangeFunction).observationRegistry(this.observationRegistry);
		this.observationRegistry.observationConfig().observationHandler(new HeaderInjectingHandler());
	}

	@Test
	void recordsObservationForSuccessfulExchange() {
		this.builder.build().get().uri("/resource/{id}", 42)
				.retrieve().bodyToMono(Void.class).block(Duration.ofSeconds(10));

		ClientRequest clientRequest = verifyAndGetRequest();

		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS")
				.hasLowCardinalityKeyValue("uri", "/resource/{id}");
		assertThat(clientRequest.headers()).containsEntry("foo", Collections.singletonList("bar"));
	}

	@Test
	void recordsObservationForSuccessfulExchangeWithParentObservationInReactorContext() {
		Observation parent = Observation.start("parent", observationRegistry);
		try {
			this.builder.build().get().uri("/resource/{id}", 42)
					.retrieve().bodyToMono(Void.class).contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, parent)).block(Duration.ofSeconds(10));
			verifyAndGetRequest();

			assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS")
					.hasParentObservationEqualTo(parent);
		}
		finally {
			parent.stop();
		}
	}

	@Test
	void recordsObservationForErrorExchange() {
		ExchangeFunction exchangeFunction = mock();
		given(exchangeFunction.exchange(any())).willReturn(Mono.error(new IllegalStateException()));
		WebClient client = WebClient.builder().observationRegistry(observationRegistry).exchangeFunction(exchangeFunction).build();
		StepVerifier.create(client.get().uri("/path").retrieve().bodyToMono(Void.class))
				.expectError(IllegalStateException.class)
				.verify(Duration.ofSeconds(5));
		assertThatHttpObservation().hasLowCardinalityKeyValue("exception", "IllegalStateException")
				.hasLowCardinalityKeyValue("status", "CLIENT_ERROR");
	}

	@Test
	void recordsObservationForCancelledExchange() {
		StepVerifier.create(this.builder.build().get().uri("/path").retrieve().bodyToMono(Void.class))
				.thenCancel()
				.verify(Duration.ofSeconds(5));
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "UNKNOWN")
				.hasLowCardinalityKeyValue("status", "CLIENT_ERROR");
	}

	@Test
	void setsCurrentObservationInReactorContext() {
		ExchangeFilterFunction assertionFilter = new ExchangeFilterFunction() {
			@Override
			public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction chain) {
				return chain.exchange(request).contextWrite(context -> {
					Observation currentObservation = context.get(ObservationThreadLocalAccessor.KEY);
					assertThat(currentObservation).isNotNull();
					assertThat(currentObservation.getContext()).isInstanceOf(ClientRequestObservationContext.class);
					return context;
				});
			}
		};
		this.builder.filter(assertionFilter).build().get().uri("/resource/{id}", 42)
				.retrieve().bodyToMono(Void.class)
				.block(Duration.ofSeconds(10));
			verifyAndGetRequest();
	}

	private TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatHttpObservation() {
		return TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.client.requests").that();
	}

	private ClientRequest verifyAndGetRequest() {
		verify(exchangeFunction).exchange(request.getValue());
		verifyNoMoreInteractions(exchangeFunction);
		return request.getValue();
	}

	static class HeaderInjectingHandler implements ObservationHandler<ClientRequestObservationContext> {

		@Override
		public void onStart(ClientRequestObservationContext context) {
			context.getSetter().set(context.getCarrier(), "foo", "bar");
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return context instanceof ClientRequestObservationContext;
		}
	}

}

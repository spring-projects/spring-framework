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

package org.springframework.web.server.adapter;


import java.util.List;
import java.util.Optional;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Observability-related tests for {@link HttpWebHandlerAdapter}.
 * @author Brian Clozel
 */
class HttpWebHandlerAdapterObservabilityTests {

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final MockServerHttpRequest request = MockServerHttpRequest.post("/test/resource").build();

	private final MockServerHttpResponse response = new MockServerHttpResponse();


	@Test
	void handlerShouldSetObservationContextOnExchange() {
		HttpStatusSuccessStubWebHandler targetHandler = new HttpStatusSuccessStubWebHandler(HttpStatus.OK);
		createWebHandler(targetHandler).handle(this.request, this.response).block();
		assertThat(targetHandler.observationContext).isPresent();
		assertThat(targetHandler.observationContext.get().getCarrier()).isEqualTo(this.request);
		assertThat(targetHandler.observationContext.get().getResponse()).isEqualTo(this.response);
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS");
	}

	@Test
	void handlerShouldSetCurrentObservationInReactorContext() {
		ReactorContextWebHandler targetHandler = new ReactorContextWebHandler();
		createWebHandler(targetHandler).handle(this.request, this.response).block();
		assertThat(targetHandler.currentObservation).isNotNull();
		assertThat(targetHandler.observationStarted).isTrue();
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS");
	}

	@Test
	void handlerShouldSeeHandledException() {
		ThrowingExceptionWebHandler throwingHandler = new ThrowingExceptionWebHandler(new IllegalStateException("testing error"));
		ExceptionHandlingWebHandler targetHandler = new ExceptionHandlingWebHandler(throwingHandler, List.of(new BadRequestExceptionHandler()));
		createWebHandler(targetHandler).handle(this.request, this.response).block();
		assertThat(throwingHandler.observationContext).isPresent();
		assertThat(throwingHandler.observationContext.get().getError()).isInstanceOf(IllegalStateException.class);
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "CLIENT_ERROR");
	}

	@Test
	void handlerShouldRecordObservationWhenCancelled() {
		HttpStatusSuccessStubWebHandler targetHandler = new HttpStatusSuccessStubWebHandler(HttpStatus.OK);
		StepVerifier.create(createWebHandler(targetHandler).handle(this.request, this.response)).thenCancel().verify();
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "UNKNOWN");
	}

	private HttpWebHandlerAdapter createWebHandler(WebHandler targetHandler) {
		HttpWebHandlerAdapter handlerAdapter = new HttpWebHandlerAdapter(targetHandler);
		handlerAdapter.setObservationRegistry(this.observationRegistry);
		return handlerAdapter;
	}

	private TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatHttpObservation() {
		return TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.server.requests").that();
	}


	private static class HttpStatusSuccessStubWebHandler implements WebHandler {

		private final HttpStatus responseStatus;

		private Optional<ServerRequestObservationContext> observationContext;

		HttpStatusSuccessStubWebHandler(HttpStatus responseStatus) {
			this.responseStatus = responseStatus;
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			this.observationContext = ServerRequestObservationContext.findCurrent(exchange.getAttributes());
			exchange.getResponse().setStatusCode(this.responseStatus);
			return Mono.empty();
		}
	}


	private static class ReactorContextWebHandler implements WebHandler {

		Observation currentObservation;

		boolean observationStarted;

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			exchange.getResponse().setStatusCode(HttpStatus.OK);
			return Mono.deferContextual(contextView -> {
				this.currentObservation = contextView.get(ObservationThreadLocalAccessor.KEY);
				this.observationStarted = this.currentObservation.getContext().getLowCardinalityKeyValue("outcome") != null;
				return Mono.empty();
			});
		}
	}


	private static class ThrowingExceptionWebHandler implements WebHandler {

		private final Throwable exception;

		private Optional<ServerRequestObservationContext> observationContext;

		private ThrowingExceptionWebHandler(Throwable exception) {
			this.exception = exception;
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			this.observationContext = ServerRequestObservationContext.findCurrent(exchange.getAttributes());
			return Mono.error(this.exception);
		}
	}


	private static class BadRequestExceptionHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
			return exchange.getResponse().setComplete();
		}
	}

}

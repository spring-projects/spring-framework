/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.server.handler;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExceptionHandlingHttpHandlerTests {

	private MockServerHttpResponse response;

	private ServerWebExchange exchange;

	private WebHandler targetHandler;


	@Before
	public void setUp() throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:8080").build();
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, this.response);
		this.targetHandler = new StubWebHandler(new IllegalStateException("boo"));
	}


	@Test
	public void handleErrorSignal() throws Exception {
		WebExceptionHandler exceptionHandler = new BadRequestExceptionHandler();
		createWebHandler(exceptionHandler).handle(this.exchange).block();

		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatusCode());
	}

	@Test
	public void handleErrorSignalWithMultipleHttpErrorHandlers() throws Exception {
		WebExceptionHandler[] exceptionHandlers = new WebExceptionHandler[] {
				new UnresolvedExceptionHandler(),
				new UnresolvedExceptionHandler(),
				new BadRequestExceptionHandler(),
				new UnresolvedExceptionHandler()
		};
		createWebHandler(exceptionHandlers).handle(this.exchange).block();

		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatusCode());
	}

	@Test
	public void unresolvedException() throws Exception {
		WebExceptionHandler exceptionHandler = new UnresolvedExceptionHandler();
		createWebHandler(exceptionHandler).handle(this.exchange).block();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatusCode());
	}

	@Test
	public void thrownExceptionBecomesErrorSignal() throws Exception {
		WebExceptionHandler exceptionHandler = new BadRequestExceptionHandler();
		createWebHandler(exceptionHandler).handle(this.exchange).block();

		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatusCode());
	}

	private WebHandler createWebHandler(WebExceptionHandler... handlers) {
		return new ExceptionHandlingWebHandler(this.targetHandler, Arrays.asList(handlers));
	}


	private static class StubWebHandler implements WebHandler {

		private final RuntimeException exception;

		private final boolean raise;


		StubWebHandler(RuntimeException exception) {
			this(exception, false);
		}

		StubWebHandler(RuntimeException exception, boolean raise) {
			this.exception = exception;
			this.raise = raise;
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			if (this.raise) {
				throw this.exception;
			}
			return Mono.error(this.exception);
		}
	}

	private static class BadRequestExceptionHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
			return Mono.empty();
		}
	}

	/** Leave the exception unresolved. */
	private static class UnresolvedExceptionHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			return Mono.error(ex);
		}
	}

}

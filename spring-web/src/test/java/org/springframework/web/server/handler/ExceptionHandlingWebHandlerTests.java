/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.server.handler;

import java.util.Arrays;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link ExceptionHandlingWebHandler}.
 * @author Rossen Stoyanchev
 */
public class ExceptionHandlingWebHandlerTests {

	private final WebHandler targetHandler = new StubWebHandler(new IllegalStateException("boo"));

	private final ServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost:8080"));


	@Test
	public void handleErrorSignal() throws Exception {
		createWebHandler(new BadRequestExceptionHandler()).handle(this.exchange).block();
		assertEquals(HttpStatus.BAD_REQUEST, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void handleErrorSignalWithMultipleHttpErrorHandlers() throws Exception {
		createWebHandler(
				new UnresolvedExceptionHandler(),
				new UnresolvedExceptionHandler(),
				new BadRequestExceptionHandler(),
				new UnresolvedExceptionHandler()).handle(this.exchange).block();

		assertEquals(HttpStatus.BAD_REQUEST, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void unresolvedException() throws Exception {
		Mono<Void> mono = createWebHandler(new UnresolvedExceptionHandler()).handle(this.exchange);
		StepVerifier.create(mono).expectErrorMessage("boo").verify();
		assertNull(this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void unresolvedExceptionWithWebHttpHandlerAdapter() throws Exception {

		// HttpWebHandlerAdapter handles unresolved errors

		new HttpWebHandlerAdapter(createWebHandler(new UnresolvedExceptionHandler()))
				.handle(this.exchange.getRequest(), this.exchange.getResponse()).block();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void thrownExceptionBecomesErrorSignal() throws Exception {
		createWebHandler(new BadRequestExceptionHandler()).handle(this.exchange).block();
		assertEquals(HttpStatus.BAD_REQUEST, this.exchange.getResponse().getStatusCode());
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

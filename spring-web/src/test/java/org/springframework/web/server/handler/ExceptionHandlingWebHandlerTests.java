/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void handleErrorSignalWithMultipleHttpErrorHandlers() throws Exception {
		createWebHandler(
				new UnresolvedExceptionHandler(),
				new UnresolvedExceptionHandler(),
				new BadRequestExceptionHandler(),
				new UnresolvedExceptionHandler()).handle(this.exchange).block();

		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void unresolvedException() throws Exception {
		Mono<Void> mono = createWebHandler(new UnresolvedExceptionHandler()).handle(this.exchange);
		StepVerifier.create(mono).expectErrorMessage("boo").verify();
		assertThat(this.exchange.getResponse().getStatusCode()).isNull();
	}

	@Test
	public void unresolvedExceptionWithWebHttpHandlerAdapter() throws Exception {

		// HttpWebHandlerAdapter handles unresolved errors

		new HttpWebHandlerAdapter(createWebHandler(new UnresolvedExceptionHandler()))
				.handle(this.exchange.getRequest(), this.exchange.getResponse()).block();

		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void thrownExceptionBecomesErrorSignal() throws Exception {
		createWebHandler(new BadRequestExceptionHandler()).handle(this.exchange).block();
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

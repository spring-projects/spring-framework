/*
 * Copyright 2002-2018 the original author or authors.
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

import java.time.Duration;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link ResponseStatusExceptionHandler}.
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionHandlerTests {

	private final ResponseStatusExceptionHandler handler = new ResponseStatusExceptionHandler();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));


	@Test
	public void handleResponseStatusException() {
		Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "");
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.BAD_REQUEST, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void handleAnnotatedException() {
		Throwable ex = new CustomException();
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.I_AM_A_TEAPOT, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void handleNestedResponseStatusException() {
		Throwable ex = new Exception(new ResponseStatusException(HttpStatus.BAD_REQUEST, ""));
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.BAD_REQUEST, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void handleNestedAnnotatedException() {
		Throwable ex = new Exception(new CustomException());
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.I_AM_A_TEAPOT, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void unresolvedException() {
		Throwable expected = new IllegalStateException();
		Mono<Void> mono = this.handler.handle(this.exchange, expected);
		StepVerifier.create(mono).consumeErrorWith(actual -> assertSame(expected, actual)).verify();
	}

	@Test // SPR-16231
	public void responseCommitted() {
		Throwable ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops");
		this.exchange.getResponse().setStatusCode(HttpStatus.CREATED);
		Mono<Void> mono = this.exchange.getResponse().setComplete()
				.then(this.handler.handle(this.exchange, ex));
		StepVerifier.create(mono).consumeErrorWith(actual -> assertSame(ex, actual)).verify();
	}


	@ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
	private static class CustomException extends Exception {
	}

}

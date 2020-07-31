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

package org.springframework.web.testfixture.server.handler;

import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for unit tests for {@link ResponseStatusExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public abstract class AbstractResponseStatusExceptionHandlerTests {

	protected final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	protected ResponseStatusExceptionHandler handler;


	@BeforeEach
	public void setup() {
		this.handler = createResponseStatusExceptionHandler();
	}

	protected ResponseStatusExceptionHandler createResponseStatusExceptionHandler() {
		return new ResponseStatusExceptionHandler();
	}


	@Test
	public void handleResponseStatusException() {
		Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "");
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void handleNestedResponseStatusException() {
		Throwable ex = new Exception(new ResponseStatusException(HttpStatus.BAD_REQUEST, ""));
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test // gh-23741
	public void handleMethodNotAllowed() {
		Throwable ex = new MethodNotAllowedException(HttpMethod.PATCH, Arrays.asList(HttpMethod.POST, HttpMethod.PUT));
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));

		MockServerHttpResponse response = this.exchange.getResponse();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(response.getHeaders().getAllow()).containsOnly(HttpMethod.POST, HttpMethod.PUT);
	}

	@Test // gh-23741
	public void handleResponseStatusExceptionWithHeaders() {
		Throwable ex = new NotAcceptableStatusException(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML));
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));

		MockServerHttpResponse response = this.exchange.getResponse();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
		assertThat(response.getHeaders().getAccept()).containsOnly(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML);
	}

	@Test
	public void unresolvedException() {
		Throwable expected = new IllegalStateException();
		Mono<Void> mono = this.handler.handle(this.exchange, expected);
		StepVerifier.create(mono).consumeErrorWith(actual -> assertThat(actual).isSameAs(expected)).verify();
	}

	@Test  // SPR-16231
	public void responseCommitted() {
		Throwable ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops");
		this.exchange.getResponse().setStatusCode(HttpStatus.CREATED);
		Mono<Void> mono = this.exchange.getResponse().setComplete()
				.then(Mono.defer(() -> this.handler.handle(this.exchange, ex)));
		StepVerifier.create(mono).consumeErrorWith(actual -> assertThat(actual).isSameAs(ex)).verify();
	}

}

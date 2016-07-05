/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.reactive;

import java.net.URI;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link ResponseStatusExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionHandlerTests {

	private ResponseStatusExceptionHandler handler;

	private MockServerHttpResponse response;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.handler = new ResponseStatusExceptionHandler();
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(
				new MockServerHttpRequest(HttpMethod.GET, new URI("/path")),
				this.response,
				new MockWebSessionManager());
	}


	@Test
	public void handleException() throws Exception {
		Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "");
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatusCode());
	}

	@Test
	public void unresolvedException() throws Exception {
		Throwable expected = new IllegalStateException();
		Mono<Void> mono = this.handler.handle(this.exchange, expected);

		TestSubscriber.subscribe(mono)
				.assertErrorWith(actual -> assertSame(expected, actual));
	}

}

/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResponseStatusExceptionHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionHandlerTests {

	private ResponseStatusExceptionHandler handler;

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;


	@Before
	public void setup() throws Exception {
		this.handler = new ResponseStatusExceptionHandler();
		this.request = MockServerHttpRequest.get("/").build();
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void handleException() throws Exception {
		Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "");
		this.handler.handle(createExchange(), ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatusCode());
	}

	@Test
	public void unresolvedException() throws Exception {
		Throwable expected = new IllegalStateException();
		Mono<Void> mono = this.handler.handle(createExchange(), expected);
		StepVerifier.create(mono).consumeErrorWith(actual -> assertSame(expected, actual)).verify();
	}


	private DefaultServerWebExchange createExchange() {
		return new DefaultServerWebExchange(this.request, this.response);
	}

}

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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Signal;
import reactor.rx.Stream;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.ResponseStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionHandlerTests {

	private ResponseStatusExceptionHandler handler;

	private MockServerHttpResponse response;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.handler = new ResponseStatusExceptionHandler();
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, this.response, sessionManager);
	}


	@Test
	public void handleException() throws Exception {
		Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
		Publisher<Void> publisher = this.handler.handle(this.exchange, ex);

		Stream.from(publisher).toList().get();
		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatus());
	}

	@Test
	public void unresolvedException() throws Exception {
		Throwable ex = new IllegalStateException();
		Publisher<Void> publisher = this.handler.handle(this.exchange, ex);

		List<Signal<Void>> signals = Stream.from(publisher).materialize().toList().get();
		assertEquals(1, signals.size());
		assertTrue(signals.get(0).hasError());
		assertSame(ex, signals.get(0).getThrowable());
	}

}

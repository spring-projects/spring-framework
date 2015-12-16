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
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Streams;
import reactor.rx.stream.Signal;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.ResponseStatusException;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionHandlerTests {

	private ResponseStatusExceptionHandler handler;

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;


	@Before
	public void setUp() throws Exception {
		this.handler = new ResponseStatusExceptionHandler();
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void handleException() throws Exception {
		Throwable ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
		Publisher<Void> publisher = this.handler.handle(this.request, this.response, ex);

		Streams.from(publisher).toList().get();
		assertEquals(HttpStatus.BAD_REQUEST, this.response.getStatus());
	}

	@Test
	public void unresolvedException() throws Exception {
		Throwable ex = new IllegalStateException();
		Publisher<Void> publisher = this.handler.handle(this.request, this.response, ex);

		List<Signal<Void>> signals = Streams.from(publisher).materialize().toList().get();
		assertEquals(1, signals.size());
		assertTrue(signals.get(0).hasError());
		assertSame(ex, signals.get(0).getThrowable());
	}

}

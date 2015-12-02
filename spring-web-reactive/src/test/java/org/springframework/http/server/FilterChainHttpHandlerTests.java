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
package org.springframework.http.server;


import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.rx.Streams;

import org.springframework.http.server.reactive.FilterChainHttpHandler;
import org.springframework.http.server.reactive.HttpFilter;
import org.springframework.http.server.reactive.HttpFilterChain;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Rossen Stoyanchev
 */
public class FilterChainHttpHandlerTests {

	private ServerHttpRequest request;

	private ServerHttpResponse response;


	@Before
	public void setUp() throws Exception {
		this.request = mock(ServerHttpRequest.class);
		this.response = mock(ServerHttpResponse.class);
	}

	@Test
	public void multipleFilters() throws Exception {
		StubHandler handler = new StubHandler();
		TestFilter filter1 = new TestFilter();
		TestFilter filter2 = new TestFilter();
		TestFilter filter3 = new TestFilter();
		FilterChainHttpHandler filterHandler = new FilterChainHttpHandler(handler, filter1, filter2, filter3);

		Publisher<Void> voidPublisher = filterHandler.handle(this.request, this.response);
		Streams.wrap(voidPublisher).toList().await(10, TimeUnit.SECONDS);

		assertTrue(filter1.invoked());
		assertTrue(filter2.invoked());
		assertTrue(filter3.invoked());
		assertTrue(handler.invoked());
	}

	@Test
	public void zeroFilters() throws Exception {
		StubHandler handler = new StubHandler();
		FilterChainHttpHandler filterHandler = new FilterChainHttpHandler(handler);

		Publisher<Void> voidPublisher = filterHandler.handle(this.request, this.response);
		Streams.wrap(voidPublisher).toList().await(10, TimeUnit.SECONDS);

		assertTrue(handler.invoked());
	}

	@Test
	public void shortcircuitFilter() throws Exception {
		StubHandler handler = new StubHandler();
		TestFilter filter1 = new TestFilter();
		ShortcircuitingFilter filter2 = new ShortcircuitingFilter();
		TestFilter filter3 = new TestFilter();
		FilterChainHttpHandler filterHandler = new FilterChainHttpHandler(handler, filter1, filter2, filter3);

		Publisher<Void> voidPublisher = filterHandler.handle(this.request, this.response);
		Streams.wrap(voidPublisher).toList().await(10, TimeUnit.SECONDS);

		assertTrue(filter1.invoked());
		assertTrue(filter2.invoked());
		assertFalse(filter3.invoked());
		assertFalse(handler.invoked());
	}


	private static class TestFilter implements HttpFilter {

		private boolean invoked;


		public boolean invoked() {
			return this.invoked;
		}

		@Override
		public Publisher<Void> filter(ServerHttpRequest req, ServerHttpResponse res,
				HttpFilterChain chain) {

			this.invoked = true;
			return doFilter(req, res, chain);
		}

		public Publisher<Void> doFilter(ServerHttpRequest req, ServerHttpResponse res,
				HttpFilterChain chain) {

			return chain.filter(req, res);
		}
	}

	private static class ShortcircuitingFilter extends TestFilter {

		@Override
		public Publisher<Void> doFilter(ServerHttpRequest req, ServerHttpResponse res,
				HttpFilterChain chain) {

			return Publishers.empty();
		}
	}

	private static class StubHandler implements HttpHandler {

		private boolean invoked;

		public boolean invoked() {
			return this.invoked;
		}

		@Override
		public Publisher<Void> handle(ServerHttpRequest req, ServerHttpResponse res) {
			this.invoked = true;
			return Publishers.empty();
		}
	}

}

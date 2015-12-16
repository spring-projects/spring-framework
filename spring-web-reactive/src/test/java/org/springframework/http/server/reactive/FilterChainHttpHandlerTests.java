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
package org.springframework.http.server.reactive;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import reactor.Mono;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Rossen Stoyanchev
 */
public class FilterChainHttpHandlerTests {

	private static Log logger = LogFactory.getLog(FilterChainHttpHandlerTests.class);


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

		filterHandler.handle(this.request, this.response).get();

		assertTrue(filter1.invoked());
		assertTrue(filter2.invoked());
		assertTrue(filter3.invoked());
		assertTrue(handler.invoked());
	}

	@Test
	public void zeroFilters() throws Exception {
		StubHandler handler = new StubHandler();
		FilterChainHttpHandler filterHandler = new FilterChainHttpHandler(handler);

		filterHandler.handle(this.request, this.response).get();

		assertTrue(handler.invoked());
	}

	@Test
	public void shortcircuitFilter() throws Exception {
		StubHandler handler = new StubHandler();
		TestFilter filter1 = new TestFilter();
		ShortcircuitingFilter filter2 = new ShortcircuitingFilter();
		TestFilter filter3 = new TestFilter();
		FilterChainHttpHandler filterHandler = new FilterChainHttpHandler(handler, filter1, filter2, filter3);

		filterHandler.handle(this.request, this.response).get();

		assertTrue(filter1.invoked());
		assertTrue(filter2.invoked());
		assertFalse(filter3.invoked());
		assertFalse(handler.invoked());
	}

	@Test
	public void asyncFilter() throws Exception {
		StubHandler handler = new StubHandler();
		AsyncFilter filter = new AsyncFilter();
		FilterChainHttpHandler filterHandler = new FilterChainHttpHandler(handler, filter);

		filterHandler.handle(this.request, this.response).get();

		assertTrue(filter.invoked());
		assertTrue(handler.invoked());
	}



	private static class TestFilter implements HttpFilter {

		private volatile boolean invoked;


		public boolean invoked() {
			return this.invoked;
		}

		@Override
		public Mono<Void> filter(ServerHttpRequest req, ServerHttpResponse res,
				HttpFilterChain chain) {

			this.invoked = true;
			return doFilter(req, res, chain);
		}

		public Mono<Void> doFilter(ServerHttpRequest req, ServerHttpResponse res,
				HttpFilterChain chain) {

			return chain.filter(req, res);
		}
	}

	private static class ShortcircuitingFilter extends TestFilter {

		@Override
		public Mono<Void> doFilter(ServerHttpRequest req, ServerHttpResponse res,
				HttpFilterChain chain) {

			return Mono.empty();
		}
	}

	private static class AsyncFilter extends TestFilter {

		@Override
		public Mono<Void> doFilter(ServerHttpRequest req, ServerHttpResponse res, HttpFilterChain chain) {
			return doAsyncWork().then(asyncResult -> {
				logger.debug("Async result: " + asyncResult);
				return chain.filter(req, res);
			});
		}

		private Mono<String> doAsyncWork() {
			return Mono.just("123");
		}
	}


	private static class StubHandler implements HttpHandler {

		private volatile boolean invoked;

		public boolean invoked() {
			return this.invoked;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest req, ServerHttpResponse res) {
			logger.trace("StubHandler invoked.");
			this.invoked = true;
			return Mono.empty();
		}
	}

}

/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link HttpHandlerAdapterSupport}.
 * @author Rossen Stoyanchev
 */
public class HttpHandlerAdapterSupportTests {


	@Test
	public void invalidContextPath() throws Exception {
		testInvalidContextPath("  ", "contextPath must not be empty");
		testInvalidContextPath("path", "contextPath must begin with '/'");
		testInvalidContextPath("/path/", "contextPath must not end with '/'");
	}

	private void testInvalidContextPath(String contextPath, String errorMessage) {
		try {
			new TestHttpHandlerAdapter(new TestHttpHandler(contextPath));
			fail();
		}
		catch (IllegalArgumentException ex) {
			assertEquals(errorMessage, ex.getMessage());
		}
	}

	@Test
	public void match() throws Exception {
		TestHttpHandler handler1 = new TestHttpHandler("/path");
		TestHttpHandler handler2 = new TestHttpHandler("/another/path");
		TestHttpHandler handler3 = new TestHttpHandler("/yet/another/path");

		testPath("/another/path/and/more", handler1, handler2, handler3);

		assertInvoked(handler2);
		assertNotInvoked(handler1, handler3);
	}

	@Test
	public void matchWithContextPathEqualToPath() throws Exception {
		TestHttpHandler handler1 = new TestHttpHandler("/path");
		TestHttpHandler handler2 = new TestHttpHandler("/another/path");
		TestHttpHandler handler3 = new TestHttpHandler("/yet/another/path");

		testPath("/path", handler1, handler2, handler3);

		assertInvoked(handler1);
		assertNotInvoked(handler2, handler3);
	}

	@Test
	public void matchWithNativeContextPath() throws Exception {
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/yet/another/path");
		request.setContextPath("/yet");

		TestHttpHandler handler = new TestHttpHandler("/another/path");
		new TestHttpHandlerAdapter(handler).handle(request);

		assertTrue(handler.wasInvoked());
		assertEquals("/yet/another/path", handler.getRequest().getContextPath());
	}

	@Test
	public void notFound() throws Exception {
		TestHttpHandler handler1 = new TestHttpHandler("/path");
		TestHttpHandler handler2 = new TestHttpHandler("/another/path");

		ServerHttpResponse response = testPath("/yet/another/path", handler1, handler2);

		assertNotInvoked(handler1, handler2);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}


	private ServerHttpResponse testPath(String path, TestHttpHandler... handlers) {
		TestHttpHandlerAdapter adapter = new TestHttpHandlerAdapter(handlers);
		return adapter.handle(path);
	}

	private void assertInvoked(TestHttpHandler handler) {
		assertTrue(handler.wasInvoked());
		assertEquals(handler.getContextPath(), handler.getRequest().getContextPath());
	}

	private void assertNotInvoked(TestHttpHandler... handlers) {
		Arrays.stream(handlers).forEach(handler -> assertFalse(handler.wasInvoked()));
	}


	@SuppressWarnings("WeakerAccess")
	private static class TestHttpHandlerAdapter extends HttpHandlerAdapterSupport {


		public TestHttpHandlerAdapter(TestHttpHandler... handlers) {
			super(initHandlerMap(handlers));
		}


		private static Map<String, HttpHandler> initHandlerMap(TestHttpHandler... testHandlers) {
			Map<String, HttpHandler> result = new LinkedHashMap<>();
			Arrays.stream(testHandlers).forEachOrdered(h -> result.put(h.getContextPath(), h));
			return result;
		}

		public ServerHttpResponse handle(String path) {
			ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, path);
			return handle(request);
		}

		public ServerHttpResponse handle(ServerHttpRequest request) {
			ServerHttpResponse response = new MockServerHttpResponse();
			getHttpHandler().handle(request, response);
			return response;
		}
	}

	@SuppressWarnings("WeakerAccess")
	private static class TestHttpHandler implements HttpHandler {

		private final String contextPath;

		private ServerHttpRequest request;


		public TestHttpHandler(String contextPath) {
			this.contextPath = contextPath;
		}


		public String getContextPath() {
			return this.contextPath;
		}

		public boolean wasInvoked() {
			return this.request != null;
		}

		public ServerHttpRequest getRequest() {
			return this.request;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			this.request = request;
			return Mono.empty();
		}
	}

}

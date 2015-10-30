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

package org.springframework.reactive.web.dispatch.method.annotation;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Deleuze
 */
public class RequestMappingHandlerMappingTests {

	private RequestMappingHandlerMapping mapping;

	@Before
	public void setup() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("handlerMapping", RequestMappingHandlerMapping.class);
		wac.registerSingleton("controller", TestController.class);
		wac.refresh();
		this.mapping = (RequestMappingHandlerMapping)wac.getBean("handlerMapping");
	}

	@Test
	public void path() throws Exception {
		ReactiveServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "boo");
		HandlerMethod handler = (HandlerMethod) this.mapping.getHandler(request);
		assertEquals(TestController.class.getMethod("boo"), handler.getMethod());
	}

	@Test
	public void method() throws Exception {
		ReactiveServerHttpRequest request = new MockServerHttpRequest(HttpMethod.POST, "foo");
		HandlerMethod handler = (HandlerMethod) this.mapping.getHandler(request);
		assertEquals(TestController.class.getMethod("postFoo"), handler.getMethod());

		request = new MockServerHttpRequest(HttpMethod.GET, "foo");
		handler = (HandlerMethod) this.mapping.getHandler(request);
		assertEquals(TestController.class.getMethod("getFoo"), handler.getMethod());

		request = new MockServerHttpRequest(HttpMethod.PUT, "foo");
		handler = (HandlerMethod) this.mapping.getHandler(request);
		assertEquals(TestController.class.getMethod("foo"), handler.getMethod());
	}


	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@RequestMapping("foo")
		public String foo() {
			return "foo";
		}

		@RequestMapping(path = "foo", method = RequestMethod.POST)
		public String postFoo() {
			return "postFoo";
		}

		@RequestMapping(path = "foo", method = RequestMethod.GET)
		public String getFoo() {
			return "getFoo";
		}

		@RequestMapping("bar")
		public String bar() {
			return "bar";
		}

		@RequestMapping("boo")
		public String boo() {
			return "boo";
		}

	}

	private static class MockServerHttpRequest implements ReactiveServerHttpRequest{

		private HttpMethod method;

		private URI uri;

		public MockServerHttpRequest(HttpMethod method, String path) {
			this.method = method;
			try {
				this.uri = new URI(path);
			} catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}

		@Override
		public Publisher<ByteBuffer> getBody() {
			return null;
		}

		@Override
		public HttpMethod getMethod() {
			return this.method;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		@Override
		public HttpHeaders getHeaders() {
			return null;
		}
	}

}

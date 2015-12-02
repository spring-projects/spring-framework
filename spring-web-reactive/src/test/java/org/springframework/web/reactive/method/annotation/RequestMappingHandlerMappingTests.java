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

package org.springframework.web.reactive.method.annotation;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Streams;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "boo");
		Publisher<?> handlerPublisher = this.mapping.getHandler(request);
		HandlerMethod handlerMethod = toHandlerMethod(handlerPublisher);
		assertEquals(TestController.class.getMethod("boo"), handlerMethod.getMethod());
	}

	@Test
	public void method() throws Exception {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.POST, "foo");
		Publisher<?> handlerPublisher = this.mapping.getHandler(request);
		HandlerMethod handlerMethod = toHandlerMethod(handlerPublisher);
		assertEquals(TestController.class.getMethod("postFoo"), handlerMethod.getMethod());

		request = new MockServerHttpRequest(HttpMethod.GET, "foo");
		handlerPublisher = this.mapping.getHandler(request);
		handlerMethod = toHandlerMethod(handlerPublisher);
		assertEquals(TestController.class.getMethod("getFoo"), handlerMethod.getMethod());
	}

	private HandlerMethod toHandlerMethod(Publisher<?> handlerPublisher) throws InterruptedException {
		assertNotNull(handlerPublisher);
		List<?> handlerList = Streams.wrap(handlerPublisher).toList().await(5, TimeUnit.SECONDS);
		assertEquals(1, handlerList.size());
		return (HandlerMethod) handlerList.get(0);
	}


	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

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


	/**
	 * TODO: this is more widely needed.
	 */
	private static class MockServerHttpRequest implements ServerHttpRequest {

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

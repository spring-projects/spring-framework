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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Sebastien Deleuze
 */
public class RequestMappingHandlerMappingTests {

	private RequestMappingHandlerMapping mapping;


	@Before
	public void setup() {
		StaticApplicationContext wac = new StaticApplicationContext();
		wac.registerSingleton("handlerMapping", RequestMappingHandlerMapping.class);
		wac.registerSingleton("controller", TestController.class);
		wac.refresh();
		this.mapping = (RequestMappingHandlerMapping)wac.getBean("handlerMapping");
	}


	@Test
	public void path() throws Exception {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("boo"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, sessionManager);
		Publisher<?> handlerPublisher = this.mapping.getHandler(exchange);
		HandlerMethod handlerMethod = toHandlerMethod(handlerPublisher);
		assertEquals(TestController.class.getMethod("boo"), handlerMethod.getMethod());
	}

	@Test
	public void method() throws Exception {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.POST, new URI("foo"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, sessionManager);
		Publisher<?> handlerPublisher = this.mapping.getHandler(exchange);
		HandlerMethod handlerMethod = toHandlerMethod(handlerPublisher);
		assertEquals(TestController.class.getMethod("postFoo"), handlerMethod.getMethod());

		request = new MockServerHttpRequest(HttpMethod.GET, new URI("foo"));
		exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
		handlerPublisher = this.mapping.getHandler(exchange);
		handlerMethod = toHandlerMethod(handlerPublisher);
		assertEquals(TestController.class.getMethod("getFoo"), handlerMethod.getMethod());
	}

	private HandlerMethod toHandlerMethod(Publisher<?> handlerPublisher) throws InterruptedException {
		assertNotNull(handlerPublisher);
		List<?> handlerList = StreamSupport.stream(Flux.from(handlerPublisher).toIterable().spliterator(), false).collect(toList());
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

}

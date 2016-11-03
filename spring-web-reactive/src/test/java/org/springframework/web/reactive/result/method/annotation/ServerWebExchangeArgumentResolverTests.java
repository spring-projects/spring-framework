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

package org.springframework.web.reactive.result.method.annotation;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ServerWebExchangeArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class ServerWebExchangeArgumentResolverTests {

	private ServerWebExchangeArgumentResolver resolver = new ServerWebExchangeArgumentResolver();

	private ServerWebExchange exchange;

	private ResolvableMethod testMethod = ResolvableMethod.onClass(getClass()).name("handle");


	@Before
	public void setUp() throws Exception {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/path");
		ServerHttpResponse response = new MockServerHttpResponse();

		WebSession session = mock(WebSession.class);
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		when(sessionManager.getSession(any())).thenReturn(Mono.just(session));

		this.exchange = new DefaultServerWebExchange(request, response, sessionManager);
	}

	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(parameter(ServerWebExchange.class)));
		assertTrue(this.resolver.supportsParameter(parameter(ServerHttpRequest.class)));
		assertTrue(this.resolver.supportsParameter(parameter(ServerHttpResponse.class)));
		assertTrue(this.resolver.supportsParameter(parameter(HttpMethod.class)));
		assertFalse(this.resolver.supportsParameter(parameter(String.class)));
	}

	@Test
	public void resolveArgument() throws Exception {
		testResolveArgument(parameter(ServerWebExchange.class), this.exchange);
		testResolveArgument(parameter(ServerHttpRequest.class), this.exchange.getRequest());
		testResolveArgument(parameter(ServerHttpResponse.class), this.exchange.getResponse());
		testResolveArgument(parameter(HttpMethod.class), HttpMethod.GET);
	}

	private void testResolveArgument(MethodParameter parameter, Object expected) {
		Mono<Object> mono = this.resolver.resolveArgument(parameter, new BindingContext(), this.exchange);
		assertSame(expected, mono.block());
	}

	private MethodParameter parameter(Class<?> parameterType) {
		return this.testMethod.resolveParam(parameter -> parameterType.equals(parameter.getParameterType()));
	}


	@SuppressWarnings("unused")
	public void handle(
			ServerWebExchange exchange,
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSession session,
			HttpMethod httpMethod,
			String s) {
	}

}

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

import java.lang.reflect.Method;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.tests.TestSubscriber;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture with {@link CookieValueMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class CookieValueMethodArgumentResolverTests {

	private CookieValueMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MethodParameter cookieParameter;
	private MethodParameter cookieStringParameter;
	private MethodParameter stringParameter;


	@Before
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ConversionService cs = new DefaultConversionService();
		this.resolver = new CookieValueMethodArgumentResolver(cs, context.getBeanFactory());

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = new MockWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		Method method = getClass().getMethod("params", HttpCookie.class, String.class, String.class);
		this.cookieParameter = new SynthesizingMethodParameter(method, 0);
		this.cookieStringParameter = new SynthesizingMethodParameter(method, 1);
		this.stringParameter = new SynthesizingMethodParameter(method, 2);
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(this.cookieParameter));
		assertTrue(this.resolver.supportsParameter(this.cookieStringParameter));
		assertFalse(this.resolver.supportsParameter(this.stringParameter));
	}

	@Test
	public void resolveCookieArgument() {
		HttpCookie expected = new HttpCookie("name", "foo");
		this.exchange.getRequest().getCookies().add(expected.getName(), expected);

		Mono<Object> mono = this.resolver.resolveArgument(this.cookieParameter, null, this.exchange);
		assertEquals(expected, mono.block());
	}

	@Test
	public void resolveCookieStringArgument() {
		HttpCookie cookie = new HttpCookie("name", "foo");
		this.exchange.getRequest().getCookies().add(cookie.getName(), cookie);

		Mono<Object> mono = this.resolver.resolveArgument(this.cookieStringParameter, null, this.exchange);
		assertEquals("Invalid result", cookie.getValue(), mono.block());
	}

	@Test
	public void resolveCookieDefaultValue() {
		Mono<Object> mono = this.resolver.resolveArgument(this.cookieStringParameter, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof String);
		assertEquals("bar", result);
	}

	@Test
	public void notFound() {
		Mono<Object> mono = resolver.resolveArgument(this.cookieParameter, null, this.exchange);
		TestSubscriber
				.subscribe(mono)
				.assertError(ServerWebInputException.class);
	}


	@SuppressWarnings("unused")
	public void params(
			@CookieValue("name") HttpCookie cookie,
			@CookieValue(name = "name", defaultValue = "bar") String cookieString,
			String stringParam) {
	}

}

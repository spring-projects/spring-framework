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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RequestHeaderMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramResolvedNameWithExpression;
	private MethodParameter paramResolvedNameWithPlaceholder;
	private MethodParameter paramNamedValueMap;
	private MethodParameter paramDate;
	private MethodParameter paramInstant;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		this.resolver = new RequestHeaderMethodArgumentResolver(conversionService, context.getBeanFactory());

		@SuppressWarnings("ConfusingArgumentToVarargsMethod")
		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		this.paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 0);
		this.paramNamedValueStringArray = new SynthesizingMethodParameter(method, 1);
		this.paramSystemProperty = new SynthesizingMethodParameter(method, 2);
		this.paramResolvedNameWithExpression = new SynthesizingMethodParameter(method, 3);
		this.paramResolvedNameWithPlaceholder = new SynthesizingMethodParameter(method, 4);
		this.paramNamedValueMap = new SynthesizingMethodParameter(method, 5);
		this.paramDate = new SynthesizingMethodParameter(method, 6);
		this.paramInstant = new SynthesizingMethodParameter(method, 7);

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = new MockWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	@Test
	public void supportsParameter() {
		assertTrue("String parameter not supported", resolver.supportsParameter(paramNamedDefaultValueStringHeader));
		assertTrue("String array parameter not supported", resolver.supportsParameter(paramNamedValueStringArray));
		assertFalse("non-@RequestParam parameter supported", resolver.supportsParameter(paramNamedValueMap));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";
		this.exchange.getRequest().getHeaders().add("name", expected);

		Mono<Object> mono = this.resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, this.exchange);
		Object result = mono.block();
		assertTrue(result instanceof String);
		assertEquals(expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		this.exchange.getRequest().getHeaders().put("name", Arrays.asList(expected));

		Mono<Object> mono = this.resolver.resolveArgument(paramNamedValueStringArray, null, this.exchange);
		Object result = mono.block();
		assertTrue(result instanceof String[]);
		assertArrayEquals(expected, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, this.exchange);
		Object result = mono.block();
		assertTrue(result instanceof String);
		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(paramSystemProperty, null, this.exchange);
			Object result = mono.block();
			assertTrue(result instanceof String);
			assertEquals("bar", result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughExpression() throws Exception {
		String expected = "foo";
		this.exchange.getRequest().getHeaders().add("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(paramResolvedNameWithExpression, null, this.exchange);
			Object result = mono.block();
			assertTrue(result instanceof String);
			assertEquals(expected, result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughPlaceholder() throws Exception {
		String expected = "foo";
		this.exchange.getRequest().getHeaders().add("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(paramResolvedNameWithPlaceholder, null, this.exchange);
			Object result = mono.block();
			assertTrue(result instanceof String);
			assertEquals(expected, result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void notFound() throws Exception {
		Mono<Object> mono = resolver.resolveArgument(paramNamedValueStringArray, null, this.exchange);
		TestSubscriber
				.subscribe(mono)
				.assertError(ServerWebInputException.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void dateConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		this.exchange.getRequest().getHeaders().add("name", rfc1123val);

		Mono<Object> mono = this.resolver.resolveArgument(paramDate, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof Date);
		assertEquals(new Date(rfc1123val), result);
	}

	@Test
	public void instantConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		this.exchange.getRequest().getHeaders().add("name", rfc1123val);

		Mono<Object> mono = this.resolver.resolveArgument(paramInstant, null, this.exchange);
		Object result = mono.block();

		assertTrue(result instanceof Instant);
		assertEquals(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)), result);
	}


	@SuppressWarnings("unused")
	public void params(
			@RequestHeader(name = "name", defaultValue = "bar") String param1,
			@RequestHeader("name") String[] param2,
			@RequestHeader(name = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
			@RequestHeader("#{systemProperties.systemProperty}") String param4,
			@RequestHeader("${systemProperty}") String param5,
			@RequestHeader("name") Map<?, ?> unsupported,
			@RequestHeader("name") Date dateParam,
			@RequestHeader("name") Instant instantParam) {
	}

}

/*
 * Copyright 2002-2017 the original author or authors.
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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RequestHeaderMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private ServerHttpRequest request;

	private BindingContext bindingContext;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramResolvedNameWithExpression;
	private MethodParameter paramResolvedNameWithPlaceholder;
	private MethodParameter paramNamedValueMap;
	private MethodParameter paramDate;
	private MethodParameter paramInstant;


	@Before
	public void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		this.resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		this.request = MockServerHttpRequest.get("/").build();

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		this.bindingContext = new BindingContext(initializer);

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		this.paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 0);
		this.paramNamedValueStringArray = new SynthesizingMethodParameter(method, 1);
		this.paramSystemProperty = new SynthesizingMethodParameter(method, 2);
		this.paramResolvedNameWithExpression = new SynthesizingMethodParameter(method, 3);
		this.paramResolvedNameWithPlaceholder = new SynthesizingMethodParameter(method, 4);
		this.paramNamedValueMap = new SynthesizingMethodParameter(method, 5);
		this.paramDate = new SynthesizingMethodParameter(method, 6);
		this.paramInstant = new SynthesizingMethodParameter(method, 7);
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
		this.request = MockServerHttpRequest.get("/").header("name", expected).build();

		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedDefaultValueStringHeader, this.bindingContext, createExchange());

		Object result = mono.block();
		assertTrue(result instanceof String);
		assertEquals(expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		this.request = MockServerHttpRequest.get("/").header("name", "foo", "bar").build();

		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedValueStringArray, this.bindingContext, createExchange());

		Object result = mono.block();
		assertTrue(result instanceof String[]);
		assertArrayEquals(new String[] {"foo", "bar"}, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedDefaultValueStringHeader, this.bindingContext, createExchange());

		Object result = mono.block();
		assertTrue(result instanceof String);
		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramSystemProperty, this.bindingContext, createExchange());

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
		this.request = MockServerHttpRequest.get("/").header("bar", expected).build();

		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramResolvedNameWithExpression, this.bindingContext, createExchange());

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
		this.request = MockServerHttpRequest.get("/").header("bar", expected).build();

		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramResolvedNameWithPlaceholder, this.bindingContext, createExchange());

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
		Mono<Object> mono = resolver.resolveArgument(
				this.paramNamedValueStringArray, this.bindingContext, createExchange());

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void dateConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		this.request = MockServerHttpRequest.get("/").header("name", rfc1123val).build();

		Mono<Object> mono = this.resolver.resolveArgument(this.paramDate, this.bindingContext, createExchange());
		Object result = mono.block();

		assertTrue(result instanceof Date);
		assertEquals(new Date(rfc1123val), result);
	}

	@Test
	public void instantConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		this.request = MockServerHttpRequest.get("/").header("name", rfc1123val).build();

		Mono<Object> mono = this.resolver.resolveArgument(this.paramInstant, this.bindingContext, createExchange());
		Object result = mono.block();

		assertTrue(result instanceof Instant);
		assertEquals(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)), result);
	}


	private DefaultServerWebExchange createExchange() {
		return new DefaultServerWebExchange(this.request, new MockServerHttpResponse());
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

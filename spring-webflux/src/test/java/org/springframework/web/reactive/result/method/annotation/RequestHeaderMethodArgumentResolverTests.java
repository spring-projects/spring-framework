/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link RequestHeaderMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private BindingContext bindingContext;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramResolvedNameWithExpression;
	private MethodParameter paramResolvedNameWithPlaceholder;
	private MethodParameter paramNamedValueMap;
	private MethodParameter paramDate;
	private MethodParameter paramInstant;
	private MethodParameter paramMono;


	@BeforeEach
	@SuppressWarnings("resource")
	public void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory(), adapterRegistry);

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
		this.paramMono = new SynthesizingMethodParameter(method, 8);
	}


	@Test
	public void supportsParameter() {
		assertThat(resolver.supportsParameter(paramNamedDefaultValueStringHeader)).as("String parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramNamedValueStringArray)).as("String array parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramNamedValueMap)).as("non-@RequestParam parameter supported").isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.paramMono))
			.withMessageStartingWith("RequestHeaderMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void resolveStringArgument() {
		String expected = "foo";
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").header("name", expected));

		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedDefaultValueStringHeader, this.bindingContext, exchange);

		Object result = mono.block();
		boolean condition = result instanceof String;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo(expected);
	}

	@Test
	public void resolveStringArrayArgument() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header("name", "foo", "bar").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedValueStringArray, this.bindingContext, exchange);

		Object result = mono.block();
		boolean condition = result instanceof String[];
		assertThat(condition).isTrue();
		assertThat((String[]) result).isEqualTo(new String[] {"foo", "bar"});
	}

	@Test
	public void resolveDefaultValue() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		Mono<Object> mono = this.resolver.resolveArgument(
				this.paramNamedDefaultValueStringHeader, this.bindingContext, exchange);

		Object result = mono.block();
		boolean condition = result instanceof String;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo("bar");
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() {
		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramSystemProperty, this.bindingContext,
					MockServerWebExchange.from(MockServerHttpRequest.get("/")));

			Object result = mono.block();
			boolean condition = result instanceof String;
			assertThat(condition).isTrue();
			assertThat(result).isEqualTo("bar");
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughExpression() {
		String expected = "foo";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header("bar", expected).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramResolvedNameWithExpression, this.bindingContext, exchange);

			Object result = mono.block();
			boolean condition = result instanceof String;
			assertThat(condition).isTrue();
			assertThat(result).isEqualTo(expected);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughPlaceholder() {
		String expected = "foo";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header("bar", expected).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		System.setProperty("systemProperty", "bar");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramResolvedNameWithPlaceholder, this.bindingContext, exchange);

			Object result = mono.block();
			boolean condition = result instanceof String;
			assertThat(condition).isTrue();
			assertThat(result).isEqualTo(expected);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void notFound() {
		Mono<Object> mono = resolver.resolveArgument(
				this.paramNamedValueStringArray, this.bindingContext,
				MockServerWebExchange.from(MockServerHttpRequest.get("/")));

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void dateConversion() {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header("name", rfc1123val).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramDate, this.bindingContext, exchange);
		Object result = mono.block();

		boolean condition = result instanceof Date;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo(new Date(rfc1123val));
	}

	@Test
	public void instantConversion() {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header("name", rfc1123val).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramInstant, this.bindingContext, exchange);
		Object result = mono.block();

		boolean condition = result instanceof Instant;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)));
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
			@RequestHeader("name") Instant instantParam,
			@RequestHeader Mono<String> alsoNotSupported) {
	}

}

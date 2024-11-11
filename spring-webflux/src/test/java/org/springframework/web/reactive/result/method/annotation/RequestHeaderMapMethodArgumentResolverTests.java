/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RequestHeaderMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class RequestHeaderMapMethodArgumentResolverTests {

	private RequestHeaderMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;
	private MethodParameter paramMultiValueMap;
	private MethodParameter paramHttpHeaders;
	private MethodParameter paramUnsupported;
	private MethodParameter paramAlsoUnsupported;


	@BeforeEach
	void setup() throws Exception {
		resolver = new RequestHeaderMapMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		paramMap = new SynthesizingMethodParameter(method, 0);
		paramMultiValueMap = new SynthesizingMethodParameter(method, 1);
		paramHttpHeaders = new SynthesizingMethodParameter(method, 2);
		paramUnsupported = new SynthesizingMethodParameter(method, 3);
		paramUnsupported = new SynthesizingMethodParameter(method, 3);
		paramAlsoUnsupported = new SynthesizingMethodParameter(method, 4);
	}


	@Test
	void supportsParameter() {
		assertThat(resolver.supportsParameter(paramMap)).as("Map parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramMultiValueMap)).as("MultiValueMap parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramHttpHeaders)).as("HttpHeaders parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramUnsupported)).as("non-@RequestParam map supported").isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.paramAlsoUnsupported))
			.withMessageStartingWith("RequestHeaderMapMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	void resolveMapArgument() {
		String name = "foo";
		String value = "bar";
		Map<String, String> expected = Collections.singletonMap(name, value);
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(name, value).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		Mono<Object> mono = resolver.resolveArgument(paramMap, null, exchange);
		Object result = mono.block();

		boolean condition = result instanceof Map;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMultiValueMapArgument() {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(name, value1, value2).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		Mono<Object> mono = resolver.resolveArgument(paramMultiValueMap, null, exchange);
		Object result = mono.block();

		assertThat(result).isInstanceOf(MultiValueMap.class);
		assertThat((MultiValueMap<String, String>) result).containsExactlyEntriesOf(expected);
	}

	@Test
	void resolveHttpHeadersArgument() {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		MockServerHttpRequest request = MockServerHttpRequest.get("/").header(name, value1, value2).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		HttpHeaders expected = new HttpHeaders();
		expected.add(name, value1);
		expected.add(name, value2);

		Mono<Object> mono = resolver.resolveArgument(paramHttpHeaders, null, exchange);
		Object result = mono.block();

		boolean condition = result instanceof HttpHeaders;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}


	@SuppressWarnings("unused")
	public void params(
			@RequestHeader Map<?, ?> param1,
			@RequestHeader MultiValueMap<?, ?> param2,
			@RequestHeader HttpHeaders param3,
			Map<?,?> unsupported,
			@RequestHeader Mono<Map<?, ?>> alsoUnsupported) {
	}

}

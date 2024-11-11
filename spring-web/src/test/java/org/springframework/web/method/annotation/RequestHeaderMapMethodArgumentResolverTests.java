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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Text fixture with {@link RequestHeaderMapMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class RequestHeaderMapMethodArgumentResolverTests {

	private RequestHeaderMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;

	private MethodParameter paramMultiValueMap;

	private MethodParameter paramHttpHeaders;

	private MethodParameter paramUnsupported;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;


	@BeforeEach
	void setup() throws Exception {
		resolver = new RequestHeaderMapMethodArgumentResolver();

		Method method = getClass().getMethod("params", Map.class, MultiValueMap.class, HttpHeaders.class, Map.class);
		paramMap = new SynthesizingMethodParameter(method, 0);
		paramMultiValueMap = new SynthesizingMethodParameter(method, 1);
		paramHttpHeaders = new SynthesizingMethodParameter(method, 2);
		paramUnsupported = new SynthesizingMethodParameter(method, 3);

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}


	@Test
	void supportsParameter() {
		assertThat(resolver.supportsParameter(paramMap)).as("Map parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramMultiValueMap)).as("MultiValueMap parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramHttpHeaders)).as("HttpHeaders parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramUnsupported)).as("non-@RequestParam map supported").isFalse();
	}

	@Test
	void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		Map<String, String> expected = Collections.singletonMap(name, value);
		request.addHeader(name, value);

		Object result = resolver.resolveArgument(paramMap, null, webRequest, null);

		boolean condition = result instanceof Map;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	void resolveMultiValueMapArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";

		request.addHeader(name, value1);
		request.addHeader(name, value2);

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		Object result = resolver.resolveArgument(paramMultiValueMap, null, webRequest, null);

		boolean condition = result instanceof MultiValueMap;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	void resolveHttpHeadersArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";

		request.addHeader(name, value1);
		request.addHeader(name, value2);

		HttpHeaders expected = new HttpHeaders();
		expected.add(name, value1);
		expected.add(name, value2);

		Object result = resolver.resolveArgument(paramHttpHeaders, null, webRequest, null);

		boolean condition = result instanceof HttpHeaders;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}


	public void params(@RequestHeader Map<?, ?> param1,
			@RequestHeader MultiValueMap<?, ?> param2, @RequestHeader HttpHeaders param3,
			Map<?, ?> unsupported) {
	}

}

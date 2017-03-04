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

package org.springframework.web.method.annotation;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ResolvableMethod;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMapMethodArgumentResolver();

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}


	@Test
	public void supportsParameter() {
		MethodParameter param = this.testMethod.annotated(RequestParam.class, name("")).arg(Map.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(MultiValueMap.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class, name("name")).arg(Map.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.notAnnotated(RequestParam.class).arg(Map.class);
		assertFalse(resolver.supportsParameter(param));
	}

	@Test
	public void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		request.addParameter(name, value);
		Map<String, String> expected = Collections.singletonMap(name, value);

		MethodParameter param = this.testMethod.annotated(RequestParam.class, name("")).arg(Map.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		assertTrue(result instanceof Map);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		request.addParameter(name, value1, value2);

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultiValueMap.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		assertTrue(result instanceof MultiValueMap);
		assertEquals("Invalid result", expected, result);
	}

	private Predicate<RequestParam> name(String name) {
		return a -> name.equals(a.name());
	}


	public void handle(
			@RequestParam Map<?, ?> param1,
			@RequestParam MultiValueMap<?, ?> param2,
			@RequestParam("name") Map<?, ?> param3,
			Map<?, ?> param4) {
	}

}

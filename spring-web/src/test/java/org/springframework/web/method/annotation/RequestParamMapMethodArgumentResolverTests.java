/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

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
import static org.springframework.web.method.MvcAnnotationPredicates.*;

/**
 * Test fixture with {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver = new RequestParamMapMethodArgumentResolver();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private NativeWebRequest webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(Map.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(Map.class);
		assertFalse(resolver.supportsParameter(param));
	}

	@Test
	public void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		request.addParameter(name, value);
		Map<String, String> expected = Collections.singletonMap(name, value);

		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(Map.class);
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

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		assertTrue(result instanceof MultiValueMap);
		assertEquals("Invalid result", expected, result);
	}


	public void handle(
			@RequestParam Map<?, ?> param1,
			@RequestParam MultiValueMap<?, ?> param2,
			@RequestParam("name") Map<?, ?> param3,
			Map<?, ?> param4) {
	}

}

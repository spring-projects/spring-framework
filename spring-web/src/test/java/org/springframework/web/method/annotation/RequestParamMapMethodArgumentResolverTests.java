/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

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
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;

/**
 * Test fixture with {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;

	private MethodParameter paramMultiValueMap;

	private MethodParameter paramNamedMap;

	private MethodParameter paramMapWithoutAnnot;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMapMethodArgumentResolver();

		Method method = getClass().getMethod("params", Map.class, MultiValueMap.class, Map.class, Map.class);
		paramMap = new MethodParameter(method, 0);
		paramMultiValueMap = new MethodParameter(method, 1);
		paramNamedMap = new MethodParameter(method, 2);
		paramMapWithoutAnnot = new MethodParameter(method, 3);

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}

	@Test
	public void supportsParameter() {
		assertTrue("Map parameter not supported", resolver.supportsParameter(paramMap));
		assertTrue("MultiValueMap parameter not supported", resolver.supportsParameter(paramMultiValueMap));
		assertFalse("Map with name supported", resolver.supportsParameter(paramNamedMap));
		assertFalse("non-@RequestParam map supported", resolver.supportsParameter(paramMapWithoutAnnot));
	}

	@Test
	public void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		request.addParameter(name, value);
		Map<String, String> expected = Collections.singletonMap(name, value);

		Object result = resolver.resolveArgument(paramMap, null, webRequest, null);

		assertTrue(result instanceof Map);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		request.addParameter(name, new String[]{value1, value2});

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<String, String>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		Object result = resolver.resolveArgument(paramMultiValueMap, null, webRequest, null);

		assertTrue(result instanceof MultiValueMap);
		assertEquals("Invalid result", expected, result);
	}

	public void params(@RequestParam Map<?, ?> param1,
					   @RequestParam MultiValueMap<?, ?> param2,
					   @RequestParam("name") Map<?, ?> param3,
					   Map<?, ?> param4) {
	}

}

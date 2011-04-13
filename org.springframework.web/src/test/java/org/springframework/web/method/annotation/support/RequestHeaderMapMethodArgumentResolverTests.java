/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.annotation.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * @author Arjen Poutsma
 */
public class RequestHeaderMapMethodArgumentResolverTests {

	private RequestHeaderMapMethodArgumentResolver resolver;

	private MethodParameter mapParameter;

	private MethodParameter multiValueMapParameter;

	private MethodParameter httpHeadersParameter;

	private MockHttpServletRequest servletRequest;

	private MethodParameter unsupportedParameter;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		resolver = new RequestHeaderMapMethodArgumentResolver();
		Method method = getClass()
				.getMethod("params", Map.class, MultiValueMap.class, HttpHeaders.class, Map.class);
		mapParameter = new MethodParameter(method, 0);
		multiValueMapParameter = new MethodParameter(method, 1);
		httpHeadersParameter = new MethodParameter(method, 2);
		unsupportedParameter = new MethodParameter(method, 3);

		servletRequest = new MockHttpServletRequest();
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);

	}

	@Test
	public void supportsParameter() {
		assertTrue("Map parameter not supported", resolver.supportsParameter(mapParameter));
		assertTrue("MultiValueMap parameter not supported", resolver.supportsParameter(multiValueMapParameter));
		assertTrue("HttpHeaders parameter not supported", resolver.supportsParameter(httpHeadersParameter));
		assertFalse("non-@RequestParam map supported", resolver.supportsParameter(unsupportedParameter));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMapArgument() throws Exception {
		String headerName = "foo";
		String headerValue = "bar";
		Map<String, String> expected = Collections.singletonMap(headerName, headerValue);
		servletRequest.addHeader(headerName, headerValue);

		Map<String, String> result = (Map<String, String>) resolver.resolveArgument(mapParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMultiValueMapArgument() throws Exception {
		String headerName = "foo";
		String headerValue1 = "bar";
		String headerValue2 = "baz";
		MultiValueMap<String, String> expected = new LinkedMultiValueMap<String, String>(1);
		expected.add(headerName, headerValue1);
		expected.add(headerName, headerValue2);
		servletRequest.addHeader(headerName, headerValue1);
		servletRequest.addHeader(headerName, headerValue2);

		MultiValueMap<String, String> result =
				(MultiValueMap<String, String>) resolver.resolveArgument(multiValueMapParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveHttpHeadersArgument() throws Exception {
		String headerName = "foo";
		String headerValue1 = "bar";
		String headerValue2 = "baz";
		HttpHeaders expected = new HttpHeaders();
		expected.add(headerName, headerValue1);
		expected.add(headerName, headerValue2);
		servletRequest.addHeader(headerName, headerValue1);
		servletRequest.addHeader(headerName, headerValue2);

		HttpHeaders result = (HttpHeaders) resolver.resolveArgument(httpHeadersParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	public void params(@RequestHeader Map<?, ?> param1,
					   @RequestHeader MultiValueMap<?, ?> param2,
					   @RequestHeader HttpHeaders param3,
					   Map<?,?> unsupported) {

	}


}

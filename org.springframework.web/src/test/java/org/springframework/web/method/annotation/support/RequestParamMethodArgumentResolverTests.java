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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Arjen Poutsma
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private MethodParameter stringParameter;

	private MethodParameter stringArrayParameter;

	private MethodParameter mapParameter;

	private MethodParameter fileParameter;

	private MethodParameter otherParameter;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;

	private MethodParameter plainParameter;

	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMethodArgumentResolver(null, true);
		Method method = getClass()
				.getMethod("params", String.class, String[].class, Map.class, MultipartFile.class, Map.class, String.class);
		stringParameter = new MethodParameter(method, 0);
		stringArrayParameter = new MethodParameter(method, 1);
		mapParameter = new MethodParameter(method, 2);
		fileParameter = new MethodParameter(method, 3);
		otherParameter = new MethodParameter(method, 4);
		plainParameter = new MethodParameter(method, 5);
		
		plainParameter.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());

		servletRequest = new MockHttpServletRequest();
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);

	}

	@Test
	public void supportsParameter() {
		assertTrue("String parameter not supported", resolver.supportsParameter(stringParameter));
		assertTrue("String array parameter not supported", resolver.supportsParameter(stringArrayParameter));
		assertTrue("Named map not parameter supported", resolver.supportsParameter(mapParameter));
		assertTrue("MultipartFile parameter not supported", resolver.supportsParameter(fileParameter));
		assertFalse("non-@RequestParam parameter supported", resolver.supportsParameter(otherParameter));
		assertTrue("Simple type params supported w/o annotations", resolver.supportsParameter(plainParameter));
		
		resolver = new RequestParamMethodArgumentResolver(null, false);
		assertFalse(resolver.supportsParameter(plainParameter));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";
		servletRequest.addParameter("name", expected);

		String result = (String) resolver.resolveArgument(stringParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		servletRequest.addParameter("name", expected);

		String[] result = (String[]) resolver.resolveArgument(stringArrayParameter, null, webRequest, null);
		assertArrayEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileArgument() throws Exception {
		MockMultipartHttpServletRequest servletRequest = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("file", "Hello World".getBytes());
		servletRequest.addFile(expected);
		webRequest = new ServletWebRequest(servletRequest);

		MultipartFile result = (MultipartFile) resolver.resolveArgument(fileParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		String result = (String) resolver.resolveArgument(stringParameter, null, webRequest, null);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = MissingServletRequestParameterException.class)
	public void notFound() throws Exception {
		String result = (String) resolver.resolveArgument(stringArrayParameter, null, webRequest, null);
		assertEquals("Invalid result", "bar", result);
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		servletRequest.setParameter("plainParam", "plainValue");
		String result = (String) resolver.resolveArgument(plainParameter, null, webRequest, null);
		assertEquals("plainValue", result);
	}

	public void params(@RequestParam(value = "name", defaultValue = "bar") String param1,
					   @RequestParam("name") String[] param2,
					   @RequestParam("name") Map<?, ?> param3,
					   @RequestParam(value = "file") MultipartFile file,
					   @RequestParam Map<?, ?> unsupported,
					   String plainParam) {
	}

}

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

import javax.servlet.http.Part;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * Test fixture with {@link RequestParamMethodArgumentResolver}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueString;
	private MethodParameter paramNamedStringArray;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMultiPartFile;
	private MethodParameter paramMap;
	private MethodParameter paramStringNotAnnot;
	private MethodParameter paramMultipartFileNotAnnot;
	private MethodParameter paramPartNotAnnot;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMethodArgumentResolver(null, true);
		
		Method method = getClass().getMethod("params", String.class, String[].class, Map.class, MultipartFile.class,
				Map.class, String.class, MultipartFile.class, Part.class);
		
		paramNamedDefaultValueString = new MethodParameter(method, 0);
		paramNamedStringArray = new MethodParameter(method, 1);
		paramNamedMap = new MethodParameter(method, 2);
		paramMultiPartFile = new MethodParameter(method, 3);
		paramMap = new MethodParameter(method, 4);
		paramStringNotAnnot = new MethodParameter(method, 5);
		paramStringNotAnnot.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramMultipartFileNotAnnot = new MethodParameter(method, 6);
		paramMultipartFileNotAnnot.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramPartNotAnnot = new MethodParameter(method, 7);
		paramPartNotAnnot.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}

	@Test
	public void supportsParameter() {
		resolver = new RequestParamMethodArgumentResolver(null, true);
		assertTrue("String parameter not supported", resolver.supportsParameter(paramNamedDefaultValueString));
		assertTrue("String array parameter not supported", resolver.supportsParameter(paramNamedStringArray));
		assertTrue("Named map not parameter supported", resolver.supportsParameter(paramNamedMap));
		assertTrue("MultipartFile parameter not supported", resolver.supportsParameter(paramMultiPartFile));
		assertFalse("non-@RequestParam parameter supported", resolver.supportsParameter(paramMap));
		assertTrue("Simple type params supported w/o annotations", resolver.supportsParameter(paramStringNotAnnot));
		assertTrue("MultipartFile parameter not supported", resolver.supportsParameter(paramMultipartFileNotAnnot));
		assertTrue("Part parameter not supported", resolver.supportsParameter(paramPartNotAnnot));
		
		resolver = new RequestParamMethodArgumentResolver(null, false);
		assertFalse(resolver.supportsParameter(paramStringNotAnnot));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";
		request.addParameter("name", expected);

		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		request.addParameter("name", expected);

		Object result = resolver.resolveArgument(paramNamedStringArray, null, webRequest, null);

		assertTrue(result instanceof String[]);
		assertArrayEquals("Invalid result", expected, (String[]) result);
	}

	@Test
	public void resolveMultipartFileArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("file", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultiPartFile, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileNotAnnotArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("paramMultipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileNotAnnot, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolvePartArgument() throws Exception {
		MockPart expected = new MockPart("paramPartNotAnnot", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPartNotAnnot, null, webRequest, null);

		assertTrue(result instanceof Part);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);
		
		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = MissingServletRequestParameterException.class)
	public void notFound() throws Exception {
		Object result = resolver.resolveArgument(paramNamedStringArray, null, webRequest, null);
		
		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		request.setParameter("paramStringNotAnnot", "plainValue");
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("plainValue", result);
	}

	public void params(@RequestParam(value = "name", defaultValue = "bar") String param1,
					   @RequestParam("name") String[] param2,
					   @RequestParam("name") Map<?, ?> param3,
					   @RequestParam(value = "file") MultipartFile param4,
					   @RequestParam Map<?, ?> param5,
					   String paramStringNotAnnot,
					   MultipartFile paramMultipartFileNotAnnot,
					   Part paramPartNotAnnot) {
	}

}

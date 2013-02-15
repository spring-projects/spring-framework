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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.RequestParamMethodArgumentResolver}.
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
	private MethodParameter paramMultipartFileList;
	private MethodParameter paramServlet30Part;
	private MethodParameter paramRequestPartAnnot;
	private MethodParameter paramRequired;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMethodArgumentResolver(null, true);

		ParameterNameDiscoverer paramNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

		Method method = getClass().getMethod("params", String.class, String[].class, Map.class, MultipartFile.class,
				Map.class, String.class, MultipartFile.class, List.class, Part.class, MultipartFile.class, String.class);

		paramNamedDefaultValueString = new MethodParameter(method, 0);
		paramNamedStringArray = new MethodParameter(method, 1);
		paramNamedMap = new MethodParameter(method, 2);
		paramMultiPartFile = new MethodParameter(method, 3);
		paramMap = new MethodParameter(method, 4);
		paramStringNotAnnot = new MethodParameter(method, 5);
		paramStringNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		paramMultipartFileNotAnnot = new MethodParameter(method, 6);
		paramMultipartFileNotAnnot.initParameterNameDiscovery(paramNameDiscoverer);
		paramMultipartFileList = new MethodParameter(method, 7);
		paramMultipartFileList.initParameterNameDiscovery(paramNameDiscoverer);
		paramServlet30Part = new MethodParameter(method, 8);
		paramServlet30Part.initParameterNameDiscovery(paramNameDiscoverer);
		paramRequestPartAnnot = new MethodParameter(method, 9);
		paramRequired = new MethodParameter(method, 10);

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
		assertTrue("Part parameter not supported", resolver.supportsParameter(paramServlet30Part));

		resolver = new RequestParamMethodArgumentResolver(null, false);
		assertFalse(resolver.supportsParameter(paramStringNotAnnot));
		assertFalse(resolver.supportsParameter(paramRequestPartAnnot));
	}

	@Test
	public void resolveString() throws Exception {
		String expected = "foo";
		request.addParameter("name", expected);

		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArray() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		request.addParameter("name", expected);

		Object result = resolver.resolveArgument(paramNamedStringArray, null, webRequest, null);

		assertTrue(result instanceof String[]);
		assertArrayEquals("Invalid result", expected, (String[]) result);
	}

	@Test
	public void resolveMultipartFile() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("file", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultiPartFile, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileNotAnnot() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileNotAnnot, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileList() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("multipartFileList", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("multipartFileList", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileList, null, webRequest, null);

		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test(expected = MultipartException.class)
	public void isMultipartRequest() throws Exception {
		resolver.resolveArgument(paramMultiPartFile, null, webRequest, null);
		fail("Expected exception: request is not a multipart request");
	}

	// SPR-9079

	@Test
	public void isMultipartRequestHttpPut() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileList", "Hello World".getBytes());
		request.addFile(expected);
		request.setMethod("PUT");
		webRequest = new ServletWebRequest(request);

		Object actual = resolver.resolveArgument(paramMultipartFileList, null, webRequest, null);

		assertTrue(actual instanceof List);
		assertEquals(expected, ((List<?>) actual).get(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingMultipartFile() throws Exception {
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		resolver.resolveArgument(paramMultiPartFile, null, webRequest, null);
		fail("Expected exception: request is not MultiPartHttpServletRequest but param is MultipartFile");
	}

	@Test
	public void resolveServlet30Part() throws Exception {
		MockPart expected = new MockPart("servlet30Part", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramServlet30Part, null, webRequest, null);

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
	public void missingRequestParam() throws Exception {
		resolver.resolveArgument(paramNamedStringArray, null, webRequest, null);
		fail("Expected exception");
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		request.setParameter("stringNotAnnot", "plainValue");
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("plainValue", result);
	}

	// SPR-8561

	@Test
	public void resolveSimpleTypeParamToNull() throws Exception {
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);
		assertNull(result);
	}

	// SPR-10180

	@Test
	public void resolveEmptyValueToDefault() throws Exception {
		this.request.addParameter("name", "");
		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);
		assertEquals("bar", result);
	}

	@Test
	public void resolveEmptyValueWithoutDefault() throws Exception {
		this.request.addParameter("stringNotAnnot", "");
		Object result = resolver.resolveArgument(paramStringNotAnnot, null, webRequest, null);
		assertEquals("", result);
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		this.request.addParameter("name", "");
		Object result = resolver.resolveArgument(paramRequired, null, webRequest, null);
		assertEquals("", result);
	}


	public void params(@RequestParam(value = "name", defaultValue = "bar") String param1,
			@RequestParam("name") String[] param2,
			@RequestParam("name") Map<?, ?> param3,
			@RequestParam(value = "file") MultipartFile param4,
			@RequestParam Map<?, ?> param5,
			String stringNotAnnot,
			MultipartFile multipartFileNotAnnot,
			List<MultipartFile> multipartFileList,
			Part servlet30Part,
			@RequestPart MultipartFile requestPartAnnot,
			@RequestParam(value = "name") String paramRequired) {
	}

}

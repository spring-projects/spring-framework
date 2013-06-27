/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.Part;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture with {@link RequestPartMethodArgumentResolver} and mock {@link HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestPartMethodArgumentResolverTests {

	private RequestPartMethodArgumentResolver resolver;

	private HttpMessageConverter<SimpleBean> messageConverter;

	private MultipartFile multipartFile1;
	private MultipartFile multipartFile2;

	private MethodParameter paramRequestPart;
	private MethodParameter paramNamedRequestPart;
	private MethodParameter paramValidRequestPart;
	private MethodParameter paramMultipartFile;
	private MethodParameter paramMultipartFileList;
	private MethodParameter paramInt;
	private MethodParameter paramMultipartFileNotAnnot;
	private MethodParameter paramServlet30Part;
	private MethodParameter paramRequestParamAnnot;

	private NativeWebRequest webRequest;

	private MockMultipartHttpServletRequest multipartRequest;

	private MockHttpServletResponse servletResponse;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {

		Method method = getClass().getMethod("handle", SimpleBean.class, SimpleBean.class, SimpleBean.class,
				MultipartFile.class, List.class, Integer.TYPE, MultipartFile.class, Part.class, MultipartFile.class);

		paramRequestPart = new MethodParameter(method, 0);
		paramRequestPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramNamedRequestPart = new MethodParameter(method, 1);
		paramValidRequestPart = new MethodParameter(method, 2);
		paramMultipartFile = new MethodParameter(method, 3);
		paramMultipartFileList = new MethodParameter(method, 4);
		paramInt = new MethodParameter(method, 5);
		paramMultipartFileNotAnnot = new MethodParameter(method, 6);
		paramMultipartFileNotAnnot.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramServlet30Part = new MethodParameter(method, 7);
		paramServlet30Part.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramRequestParamAnnot = new MethodParameter(method, 8);

		messageConverter = mock(HttpMessageConverter.class);
		given(messageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));

		resolver = new RequestPartMethodArgumentResolver(Collections.<HttpMessageConverter<?>>singletonList(messageConverter));
		reset(messageConverter);

		multipartFile1 = new MockMultipartFile("requestPart", "", "text/plain", (byte[]) null);
		multipartFile2 = new MockMultipartFile("requestPart", "", "text/plain", (byte[]) null);
		multipartRequest = new MockMultipartHttpServletRequest();
		multipartRequest.addFile(multipartFile1);
		multipartRequest.addFile(multipartFile2);
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(multipartRequest, servletResponse);
	}

	@Test
	public void supportsParameter() {
		assertTrue("RequestPart parameter not supported", resolver.supportsParameter(paramRequestPart));
		assertTrue("MultipartFile parameter not supported", resolver.supportsParameter(paramMultipartFileNotAnnot));
		assertTrue("Part parameter not supported", resolver.supportsParameter(paramServlet30Part));
		assertFalse("non-RequestPart parameter supported", resolver.supportsParameter(paramInt));
		assertFalse("@RequestParam args not supported", resolver.supportsParameter(paramRequestParamAnnot));
	}

	@Test
	public void resolveMultipartFile() throws Exception {
		Object actual = resolver.resolveArgument(paramMultipartFile, null, webRequest, null);
		assertNotNull(actual);
		assertSame(multipartFile1, actual);
	}

	@Test
	public void resolveMultipartFileList() throws Exception {
		Object actual = resolver.resolveArgument(paramMultipartFileList, null, webRequest, null);
		assertNotNull(actual);
		assertTrue(actual instanceof List);
		assertEquals(Arrays.asList(multipartFile1, multipartFile2), actual);
	}

	@Test
	public void resolveMultipartFileNotAnnotArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileNotAnnot, null, webRequest, null);

		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveServlet30PartArgument() throws Exception {
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
	public void resolveRequestPart() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramRequestPart);
	}

	@Test
	public void resolveNamedRequestPart() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramNamedRequestPart);
	}

	@Test
	public void resolveRequestPartNotValid() throws Exception {
		try {
			testResolveArgument(new SimpleBean(null), paramValidRequestPart);
			fail("Expected exception");
		} catch (MethodArgumentNotValidException e) {
			assertEquals("requestPart", e.getBindingResult().getObjectName());
			assertEquals(1, e.getBindingResult().getErrorCount());
			assertNotNull(e.getBindingResult().getFieldError("name"));
		}
	}

	@Test
	public void resolveRequestPartValid() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramNamedRequestPart);
	}

	@Test
	public void resolveRequestPartRequired() throws Exception {
		try {
			testResolveArgument(null, paramValidRequestPart);
			fail("Expected exception");
		} catch (MissingServletRequestPartException e) {
			assertEquals("requestPart", e.getRequestPartName());
		}
	}

	@Test
	public void resolveRequestPartNotRequired() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramValidRequestPart);
	}

	@Test(expected=MultipartException.class)
	public void isMultipartRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		resolver.resolveArgument(paramMultipartFile, new ModelAndViewContainer(), new ServletWebRequest(request), null);
		fail("Expected exception");
	}

	// SPR-9079

	@Test
	public void isMultipartRequestPut() throws Exception {
		this.multipartRequest.setMethod("PUT");
		Object actual = resolver.resolveArgument(paramMultipartFile, null, webRequest, null);
		assertNotNull(actual);
		assertSame(multipartFile1, actual);
	}

	private void testResolveArgument(SimpleBean argValue, MethodParameter parameter) throws IOException, Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;

		given(messageConverter.canRead(SimpleBean.class, contentType)).willReturn(true);
		given(messageConverter.read(eq(SimpleBean.class), isA(RequestPartServletServerHttpRequest.class))).willReturn(argValue);

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		Object actualValue = resolver.resolveArgument(parameter, mavContainer, webRequest, new ValidatingBinderFactory());

		assertEquals("Invalid argument value", argValue, actualValue);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
	}

	private static class SimpleBean {

		@NotNull
		private final String name;

		public SimpleBean(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}
	}

	private final class ValidatingBinderFactory implements WebDataBinderFactory {
		@Override
		public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}

	public void handle(@RequestPart SimpleBean requestPart,
					   @RequestPart(value="requestPart", required=false) SimpleBean namedRequestPart,
					   @Valid @RequestPart("requestPart") SimpleBean validRequestPart,
					   @RequestPart("requestPart") MultipartFile multipartFile,
					   @RequestPart("requestPart") List<MultipartFile> multipartFileList,
					   int i,
					   MultipartFile multipartFileNotAnnot,
					   Part servlet30Part,
					   @RequestParam MultipartFile requestParamAnnot) {
	}

}

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

package org.springframework.web.servlet.mvc.method.annotation.support;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.RequestPartServletServerHttpRequest;

/**
 * Test fixture with {@link RequestPartMethodArgumentResolver} and mock {@link HttpMessageConverter}.
 * 
 * @author Rossen Stoyanchev
 */
public class RequestPartMethodArgumentResolverTests {

	private RequestPartMethodArgumentResolver resolver;

	private HttpMessageConverter<SimpleBean> messageConverter;
	
	private MultipartFile multipartFile;

	private MethodParameter paramRequestPart;
	private MethodParameter paramNamedRequestPart;
	private MethodParameter paramValidRequestPart;
	private MethodParameter paramInt;

	private NativeWebRequest webRequest;

	private MockMultipartHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		Method handle = getClass().getMethod("handle", SimpleBean.class, SimpleBean.class, SimpleBean.class, Integer.TYPE);
		paramRequestPart = new MethodParameter(handle, 0);
		paramRequestPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramNamedRequestPart = new MethodParameter(handle, 1);
		paramValidRequestPart = new MethodParameter(handle, 2);
		paramInt = new MethodParameter(handle, 3);

		messageConverter = createMock(HttpMessageConverter.class);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		replay(messageConverter);

		resolver = new RequestPartMethodArgumentResolver(Collections.<HttpMessageConverter<?>>singletonList(messageConverter));
		reset(messageConverter);
		
		multipartFile = new MockMultipartFile("requestPart", "", "text/plain", (byte[]) null);
		servletRequest = new MockMultipartHttpServletRequest();
		servletRequest.addFile(multipartFile);
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void supportsParameter() {
		assertTrue("RequestPart parameter not supported", resolver.supportsParameter(paramRequestPart));
		assertFalse("non-RequestPart parameter supported", resolver.supportsParameter(paramInt));
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
		} catch (RequestPartNotValidException e) {
			assertEquals("requestPart", e.getErrors().getObjectName());
			assertEquals(1, e.getErrors().getErrorCount());
			assertNotNull(e.getErrors().getFieldError("name"));
		}
	}
	
	@Test
	public void resolveRequestPartValid() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramValidRequestPart);
	}
	
	private void testResolveArgument(SimpleBean expectedValue, MethodParameter parameter) throws IOException, Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.canRead(SimpleBean.class, contentType)).andReturn(true);
		expect(messageConverter.read(eq(SimpleBean.class), isA(RequestPartServletServerHttpRequest.class))).andReturn(expectedValue);
		replay(messageConverter);

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		Object actualValue = resolver.resolveArgument(parameter, mavContainer, webRequest, new ValidatingBinderFactory());

		assertEquals("Invalid argument value", expectedValue, actualValue);
		assertTrue("The ResolveView flag shouldn't change", mavContainer.isResolveView());
		
		verify(messageConverter);
	}	

	public void handle(@RequestPart SimpleBean requestPart, 
					   @RequestPart("requestPart") SimpleBean namedRequestPart, 
					   @Valid @RequestPart("requestPart") SimpleBean validRequestPart, 
					   int i) {
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
		public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}
	
}

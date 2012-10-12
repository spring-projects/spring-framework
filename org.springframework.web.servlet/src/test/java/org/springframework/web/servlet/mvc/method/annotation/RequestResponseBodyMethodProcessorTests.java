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

package org.springframework.web.servlet.mvc.method.annotation;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Test fixture with {@link RequestResponseBodyMethodProcessor} and mock {@link HttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestResponseBodyMethodProcessorTests {

	private RequestResponseBodyMethodProcessor processor;

	private HttpMessageConverter<String> messageConverter;

	private MethodParameter paramRequestBodyString;
	private MethodParameter paramInt;
	private MethodParameter paramValidBean;
	private MethodParameter returnTypeString;
	private MethodParameter returnTypeInt;
	private MethodParameter returnTypeStringProduces;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		messageConverter = createMock(HttpMessageConverter.class);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		replay(messageConverter);

		processor = new RequestResponseBodyMethodProcessor(Collections.<HttpMessageConverter<?>>singletonList(messageConverter));
		reset(messageConverter);

		Method handle = getClass().getMethod("handle1", String.class, Integer.TYPE);
		paramRequestBodyString = new MethodParameter(handle, 0);
		paramInt = new MethodParameter(handle, 1);
		returnTypeString = new MethodParameter(handle, -1);
		returnTypeInt = new MethodParameter(getClass().getMethod("handle2"), -1);
		returnTypeStringProduces = new MethodParameter(getClass().getMethod("handle3"), -1);
		paramValidBean = new MethodParameter(getClass().getMethod("handle4", SimpleBean.class), 0);

		mavContainer = new ModelAndViewContainer();

		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void supportsParameter() {
		assertTrue("RequestBody parameter not supported", processor.supportsParameter(paramRequestBodyString));
		assertFalse("non-RequestBody parameter supported", processor.supportsParameter(paramInt));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseBody return type not supported", processor.supportsReturnType(returnTypeString));
		assertFalse("non-ResponseBody return type supported", processor.supportsReturnType(returnTypeInt));
	}

	@Test
	public void resolveArgument() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		String body = "Foo";
		expect(messageConverter.canRead(String.class, contentType)).andReturn(true);
		expect(messageConverter.read(eq(String.class), isA(HttpInputMessage.class))).andReturn(body);
		replay(messageConverter);

		Object result = processor.resolveArgument(paramRequestBodyString, mavContainer, webRequest, null);

		assertEquals("Invalid argument", body, result);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
		verify(messageConverter);
	}

	@Test
	public void resolveArgumentNotValid() throws Exception {
		try {
			testResolveArgumentWithValidation(new SimpleBean(null));
			fail("Expected exception");
		} catch (MethodArgumentNotValidException e) {
			assertEquals("simpleBean", e.getBindingResult().getObjectName());
			assertEquals(1, e.getBindingResult().getErrorCount());
			assertNotNull(e.getBindingResult().getFieldError("name"));
		}
	}

	@Test
	public void resolveArgumentValid() throws Exception {
		testResolveArgumentWithValidation(new SimpleBean("name"));
	}

	private void testResolveArgumentWithValidation(SimpleBean simpleBean) throws IOException, Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		@SuppressWarnings("unchecked")
		HttpMessageConverter<SimpleBean> beanConverter = createMock(HttpMessageConverter.class);
		expect(beanConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(beanConverter.canRead(SimpleBean.class, contentType)).andReturn(true);
		expect(beanConverter.read(eq(SimpleBean.class), isA(HttpInputMessage.class))).andReturn(simpleBean);
		replay(beanConverter);

		processor = new RequestResponseBodyMethodProcessor(Collections.<HttpMessageConverter<?>>singletonList(beanConverter));
		processor.resolveArgument(paramValidBean, mavContainer, webRequest, new ValidatingBinderFactory());

		verify(beanConverter);
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNotReadable() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.canRead(String.class, contentType)).andReturn(false);
		replay(messageConverter);

		processor.resolveArgument(paramRequestBodyString, mavContainer, webRequest, null);

		fail("Expected exception");
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNoContentType() throws Exception {
		processor.resolveArgument(paramRequestBodyString, mavContainer, webRequest, null);
		fail("Expected exception");
	}

	@Test
	public void handleReturnValue() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		String body = "Foo";
		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(true);
		messageConverter.write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
		replay(messageConverter);

		processor.handleReturnValue(body, returnTypeString, mavContainer, webRequest);

		assertTrue("The requestHandled flag wasn't set", mavContainer.isRequestHandled());
		verify(messageConverter);
	}

	@Test
	public void handleReturnValueProduces() throws Exception {
		String body = "Foo";

		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));

		expect(messageConverter.canWrite(String.class, MediaType.TEXT_HTML)).andReturn(true);
		messageConverter.write(eq(body), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
		replay(messageConverter);

		processor.handleReturnValue(body, returnTypeStringProduces, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(messageConverter);
	}


	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptable() throws Exception {
		MediaType accepted = MediaType.APPLICATION_ATOM_XML;
		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		replay(messageConverter);

		processor.handleReturnValue("Foo", returnTypeString, mavContainer, webRequest);

		fail("Expected exception");
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptableProduces() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		replay(messageConverter);

		processor.handleReturnValue("Foo", returnTypeStringProduces, mavContainer, webRequest);

		fail("Expected exception");
	}

	// SPR-9841

	@Test
	public void handleReturnValueMediaTypeSuffix() throws Exception {
		String body = "Foo";
		MediaType accepted = MediaType.APPLICATION_XHTML_XML;
		List<MediaType> supported = Collections.singletonList(MediaType.valueOf("application/*+xml"));

		servletRequest.addHeader("Accept", accepted);

		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(supported);
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(true);
		messageConverter.write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
		replay(messageConverter);

		processor.handleReturnValue(body, returnTypeStringProduces, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(messageConverter);
	}

	// SPR-9160

	@Test
	public void handleReturnValueSortByQuality() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		converters.add(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor handler = new RequestResponseBodyMethodProcessor(converters);

		handler.writeWithMessageConverters("Foo", returnTypeStringProduces, webRequest);

		assertEquals("application/json;charset=UTF-8", servletResponse.getHeader("Content-Type"));
	}

	@Test
	public void handleReturnValueString() throws Exception {
		List<HttpMessageConverter<?>>converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());

		processor = new RequestResponseBodyMethodProcessor(converters);
		processor.handleReturnValue("Foo", returnTypeString, mavContainer, webRequest);

		assertEquals("text/plain;charset=ISO-8859-1", servletResponse.getHeader("Content-Type"));
		assertEquals("Foo", servletResponse.getContentAsString());
	}

	@Test
	public void handleReturnValueStringAcceptCharset() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain;charset=UTF-8");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		processor.writeWithMessageConverters("Foo", returnTypeString, webRequest);

		assertEquals("text/plain;charset=UTF-8", servletResponse.getHeader("Content-Type"));
	}


	@ResponseBody
	public String handle1(@RequestBody String s, int i) {
		return s;
	}

	public int handle2() {
		return 42;
	}

	@ResponseBody
	public String handle3() {
		return null;
	}

	public void handle4(@Valid @RequestBody SimpleBean b) {
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

	@SuppressWarnings("unused")
	private static class SimpleBean {

		@NotNull
		private final String name;

		public SimpleBean(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
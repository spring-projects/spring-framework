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
package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Test fixture for {@link ExceptionHandlerSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class ExceptionHandlerSupportTests {

	private ExceptionHandlerSupport exceptionHandlerSupport;

	private DefaultHandlerExceptionResolver defaultExceptionResolver;

	private WebRequest request;

	private HttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;


	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.request = new ServletWebRequest(this.servletRequest, this.servletResponse);

		this.exceptionHandlerSupport = new ApplicationExceptionHandler();
		this.defaultExceptionResolver = new DefaultHandlerExceptionResolver();
	}

	@Test
	public void supportsAllDefaultHandlerExceptionResolverExceptionTypes() throws Exception {

		Method annotMethod = ExceptionHandlerSupport.class.getMethod("handleException", Exception.class, WebRequest.class);
		ExceptionHandler annot = annotMethod.getAnnotation(ExceptionHandler.class);
		List<Class<? extends Throwable>> supportedTypes = Arrays.asList(annot.value());

		for (Method method : DefaultHandlerExceptionResolver.class.getDeclaredMethods()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			if (method.getName().startsWith("handle") && (paramTypes.length == 4)) {
				String name = paramTypes[0].getSimpleName();
				assertTrue("@ExceptionHandler is missing " + name, supportedTypes.contains(paramTypes[0]));
			}
		}
	}

	@Test
	public void noSuchRequestHandlingMethod() {
		Exception ex = new NoSuchRequestHandlingMethodException("GET", TestController.class);
		testException(ex);
	}

	@Test
	public void httpRequestMethodNotSupported() {
		List<String> supported = Arrays.asList("POST", "DELETE");
		Exception ex = new HttpRequestMethodNotSupportedException("GET", supported);

		ResponseEntity<Object> responseEntity = testException(ex);
		assertEquals(EnumSet.of(HttpMethod.POST, HttpMethod.DELETE), responseEntity.getHeaders().getAllow());

	}

	@Test
	public void handleHttpMediaTypeNotSupported() {
		List<MediaType> acceptable = Arrays.asList(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML);
		Exception ex = new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_JSON, acceptable);

		ResponseEntity<Object> responseEntity = testException(ex);
		assertEquals(acceptable, responseEntity.getHeaders().getAccept());
	}

	@Test
	public void httpMediaTypeNotAcceptable() {
		Exception ex = new HttpMediaTypeNotAcceptableException("");
		testException(ex);
	}

	@Test
	public void missingServletRequestParameter() {
		Exception ex = new MissingServletRequestParameterException("param", "type");
		testException(ex);
	}

	@Test
	public void servletRequestBindingException() {
		Exception ex = new ServletRequestBindingException("message");
		testException(ex);
	}

	@Test
	public void conversionNotSupported() {
		Exception ex = new ConversionNotSupportedException(new Object(), Object.class, null);
		testException(ex);
	}

	@Test
	public void typeMismatch() {
		Exception ex = new TypeMismatchException("foo", String.class);
		testException(ex);
	}

	@Test
	public void httpMessageNotReadable() {
		Exception ex = new HttpMessageNotReadableException("message");
		testException(ex);
	}

	@Test
	public void httpMessageNotWritable() {
		Exception ex = new HttpMessageNotWritableException("");
		testException(ex);
	}

	@Test
	public void methodArgumentNotValid() {
		Exception ex = new MethodArgumentNotValidException(null, null);
		testException(ex);
	}

	@Test
	public void missingServletRequestPart() {
		Exception ex = new MissingServletRequestPartException("partName");
		testException(ex);
	}

	@Test
	public void bindException() {
		Exception ex = new BindException(new Object(), "name");
		testException(ex);
	}

	@Test
	public void controllerAdvice() throws Exception {
		StaticWebApplicationContext cxt = new StaticWebApplicationContext();
		cxt.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
		cxt.refresh();

		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setApplicationContext(cxt);
		resolver.afterPropertiesSet();

		ServletRequestBindingException ex = new ServletRequestBindingException("message");
		resolver.resolveException(this.servletRequest, this.servletResponse, null, ex);

		assertEquals(400, this.servletResponse.getStatus());
		assertEquals("error content", this.servletResponse.getContentAsString());
		assertEquals("someHeaderValue", this.servletResponse.getHeader("someHeader"));
	}


	private ResponseEntity<Object> testException(Exception ex) {
		ResponseEntity<Object> responseEntity = this.exceptionHandlerSupport.handleException(ex, this.request);
		this.defaultExceptionResolver.resolveException(this.servletRequest, this.servletResponse, null, ex);

		assertEquals(this.servletResponse.getStatus(), responseEntity.getStatusCode().value());

		return responseEntity;
	}


	private static class TestController {
	}

	@ControllerAdvice
	private static class ApplicationExceptionHandler extends ExceptionHandlerSupport {

		@Override
		protected Object handleServletRequestBindingException(ServletRequestBindingException ex,
				HttpHeaders headers, HttpStatus status, WebRequest request) {

			headers.set("someHeader", "someHeaderValue");
			return "error content";
		}


	}

}

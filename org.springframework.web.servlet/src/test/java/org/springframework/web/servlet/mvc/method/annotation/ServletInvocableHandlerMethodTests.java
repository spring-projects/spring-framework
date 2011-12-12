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
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture with {@link ServletInvocableHandlerMethod}.
 * 
 * @author Rossen Stoyanchev
 */
public class ServletInvocableHandlerMethodTests {

	private HandlerMethodArgumentResolverComposite argumentResolvers;

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
		argumentResolvers = new HandlerMethodArgumentResolverComposite();
		mavContainer = new ModelAndViewContainer();
		response = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
	}

	@Test
	public void nullReturnValueResponseStatus() throws Exception {
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod("responseStatus");
		handlerMethod.invokeAndHandle(webRequest, mavContainer);

		assertTrue("Null return value + @ResponseStatus should result in 'request handled'",
				mavContainer.isRequestHandled());

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertEquals("400 Bad Request", response.getErrorMessage());
	}

	@Test
	public void nullReturnValueHttpServletResponseArg() throws Exception {
		argumentResolvers.addResolver(new ServletResponseMethodArgumentResolver());

		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod("httpServletResponse", HttpServletResponse.class);
		handlerMethod.invokeAndHandle(webRequest, mavContainer);

		assertTrue("Null return value + HttpServletResponse arg should result in 'request handled'",
				mavContainer.isRequestHandled());
	}

	@Test
	public void nullReturnValueRequestNotModified() throws Exception {
		webRequest.getNativeRequest(MockHttpServletRequest.class).addHeader("If-Modified-Since", 10 * 1000 * 1000);
		int lastModifiedTimestamp = 1000 * 1000;
		webRequest.checkNotModified(lastModifiedTimestamp);
		
		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod("notModified");
		handlerMethod.invokeAndHandle(webRequest, mavContainer);

		assertTrue("Null return value + 'not modified' request should result in 'request handled'",
				mavContainer.isRequestHandled());
	}
	
	@Test
	public void exceptionWhileHandlingReturnValue() throws Exception {
		returnValueHandlers.addHandler(new ExceptionRaisingReturnValueHandler());

		ServletInvocableHandlerMethod handlerMethod = getHandlerMethod("handle");
		try {
			handlerMethod.invokeAndHandle(webRequest, mavContainer);
			fail("Expected exception");
		} catch (HttpMessageNotWritableException ex) {
			// Expected..
			// Allow HandlerMethodArgumentResolver exceptions to propagate..
		}
	}

	private ServletInvocableHandlerMethod getHandlerMethod(String methodName, Class<?>... argTypes) 
			throws NoSuchMethodException {
		Method method = Handler.class.getDeclaredMethod(methodName, argTypes);
		ServletInvocableHandlerMethod handlerMethod = new ServletInvocableHandlerMethod(new Handler(), method);
		handlerMethod.setHandlerMethodArgumentResolvers(argumentResolvers);
		handlerMethod.setHandlerMethodReturnValueHandlers(returnValueHandlers);
		return handlerMethod;
	}

	@SuppressWarnings("unused")
	private static class Handler {

		public String handle() {
			return "view";
		}

		@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "400 Bad Request")
		public void responseStatus() {
		}
		
		public void httpServletResponse(HttpServletResponse response) {
		}
		
		public void notModified() {
		}
		
	}

	private static class ExceptionRaisingReturnValueHandler implements HandlerMethodReturnValueHandler {

		public boolean supportsReturnType(MethodParameter returnType) {
			return true;
		}

		public void handleReturnValue(Object returnValue, MethodParameter returnType,
				ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
			throw new HttpMessageNotWritableException("oops, can't write");
		}
	}

}
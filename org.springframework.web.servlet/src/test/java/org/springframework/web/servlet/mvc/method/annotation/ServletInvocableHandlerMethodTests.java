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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletResponseMethodArgumentResolver;

/**
 * Test fixture with {@link ServletInvocableHandlerMethod}.
 * 
 * @author Rossen Stoyanchev
 */
public class ServletInvocableHandlerMethodTests {

	private final Object handler = new Handler();

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
	public void setResponseStatus() throws Exception {
		returnValueHandlers.addHandler(new ExceptionThrowingReturnValueHandler());
		handlerMethod("responseStatus").invokeAndHandle(webRequest, mavContainer);

		assertFalse("Null return value with an @ResponseStatus should result in 'no view resolution'",
				mavContainer.isResolveView());

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertEquals("400 Bad Request", response.getErrorMessage());
	}

	@Test
	public void checkNoViewResolutionWithHttpServletResponse() throws Exception {
		argumentResolvers.addResolver(new ServletResponseMethodArgumentResolver());
		returnValueHandlers.addHandler(new ExceptionThrowingReturnValueHandler());
		handlerMethod("httpServletResponse", HttpServletResponse.class).invokeAndHandle(webRequest, mavContainer);

		assertFalse("Null return value with an HttpServletResponse argument should result in 'no view resolution'",
				mavContainer.isResolveView());
	}

	@Test
	public void checkNoViewResolutionWithRequestNotModified() throws Exception {
		returnValueHandlers.addHandler(new ExceptionThrowingReturnValueHandler());
		
		webRequest.getNativeRequest(MockHttpServletRequest.class).addHeader("If-Modified-Since", 10 * 1000 * 1000);
		int lastModifiedTimestamp = 1000 * 1000;
		webRequest.checkNotModified(lastModifiedTimestamp);
		
		handlerMethod("notModified").invokeAndHandle(webRequest, mavContainer);

		assertFalse("Null return value with a 'not modified' request should result in 'no view resolution'",
				mavContainer.isResolveView());
	}

	private ServletInvocableHandlerMethod handlerMethod(String methodName, Class<?>...paramTypes)
			throws NoSuchMethodException {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		ServletInvocableHandlerMethod handlerMethod = new ServletInvocableHandlerMethod(handler, method);
		handlerMethod.setHandlerMethodArgumentResolvers(argumentResolvers);
		handlerMethod.setHandlerMethodReturnValueHandlers(returnValueHandlers);
		return handlerMethod;
	}

	private static class ExceptionThrowingReturnValueHandler implements HandlerMethodReturnValueHandler {

		public boolean supportsReturnType(MethodParameter returnType) {
			return true;
		}

		public void handleReturnValue(Object returnValue, MethodParameter returnType,
				ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
			throw new IllegalStateException("Should never be invoked");
		}
	}
	
	@SuppressWarnings("unused")
	private static class Handler {
		
		@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "400 Bad Request")
		public void responseStatus() {
		}
		
		public void httpServletResponse(HttpServletResponse response) {
		}
		
		public void notModified() {
		}
	}

}
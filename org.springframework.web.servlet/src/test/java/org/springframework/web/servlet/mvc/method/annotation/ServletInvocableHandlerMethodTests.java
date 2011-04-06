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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.mvc.method.annotation.support.DefaultMethodReturnValueHandler;

/**
 * Test fixture for {@link ServletInvocableHandlerMethod} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ServletInvocableHandlerMethodTests {

	private ServletWebRequest webRequest;

	private MockHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
	}

	@Test
	public void setResponseStatus() throws Exception {
		HandlerMethodReturnValueHandlerComposite handlers = new HandlerMethodReturnValueHandlerComposite();
		handlers.registerReturnValueHandler(new DefaultMethodReturnValueHandler(null));

		Method method = Handler.class.getDeclaredMethod("responseStatus");
		ServletInvocableHandlerMethod handlerMethod = new ServletInvocableHandlerMethod(new Handler(), method);
		handlerMethod.setHandlerMethodReturnValueHandlers(handlers);

		handlerMethod.invokeAndHandle(webRequest, null);

		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		assertEquals("400 Bad Request", response.getErrorMessage());
	}

	private static class Handler {
		
		@SuppressWarnings("unused")
		@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "400 Bad Request")
		public void responseStatus() {
		}
	}
}

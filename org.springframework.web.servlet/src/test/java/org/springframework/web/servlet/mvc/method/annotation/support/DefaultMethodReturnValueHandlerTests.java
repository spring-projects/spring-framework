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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture for {@link DefaultMethodReturnValueHandler} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class DefaultMethodReturnValueHandlerTests {

	private DefaultMethodReturnValueHandler handler;

	private ServletWebRequest webRequest;

	private ModelAndViewContainer mavContainer;

	@Before
	public void setUp() {
		this.handler = new DefaultMethodReturnValueHandler(null);
		this.mavContainer = new ModelAndViewContainer(new ExtendedModelMap());
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void returnSimpleType() throws Exception {
		handler.handleReturnValue(55, createMethodParam("simpleType"), mavContainer, webRequest);
	}

	@Test
	public void returnVoid() throws Exception {
		handler.handleReturnValue(null, null, mavContainer, webRequest);
		assertNull(mavContainer.getView());
		assertNull(mavContainer.getViewName());
		assertTrue(mavContainer.getModel().isEmpty());
	}
	
	@Test
	public void returnSingleModelAttribute() throws Exception{
		handler.handleReturnValue(new TestBean(), createMethodParam("singleModelAttribute"), mavContainer, webRequest);
		assertTrue(mavContainer.containsAttribute("testBean"));
	}

	private MethodParameter createMethodParam(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}
	
	@SuppressWarnings("unused")
	private int simpleType() {
		return 0;
	}

	@SuppressWarnings("unused")
	private void voidReturnValue() {
	}
	
	@SuppressWarnings("unused")
	private TestBean singleModelAttribute() {
		return null;
	}
	
}

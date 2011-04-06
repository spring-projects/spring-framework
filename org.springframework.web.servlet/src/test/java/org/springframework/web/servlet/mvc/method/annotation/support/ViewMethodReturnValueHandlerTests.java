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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.support.DefaultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.ViewMethodReturnValueHandler;
import org.springframework.web.servlet.view.InternalResourceView;

/**
 * Test fixture for {@link DefaultMethodReturnValueHandler} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ViewMethodReturnValueHandlerTests {

	private ViewMethodReturnValueHandler handler;

	private ServletWebRequest webRequest;

	private ModelAndViewContainer<View> mavContainer;

	@Before
	public void setUp() {
		this.handler = new ViewMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer<View>(new ExtendedModelMap());
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}
	
	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(handler.supportsReturnType(createMethodParam("view")));
		assertTrue(handler.supportsReturnType(createMethodParam("viewName")));
	}

	@Test
	public void returnView() throws Exception {
		InternalResourceView view = new InternalResourceView("testView");
		handler.handleReturnValue(view, createMethodParam("view"), mavContainer, webRequest);
		assertSame(view, mavContainer.getView());
	}
	
	@Test
	public void returnViewName() throws Exception {
		handler.handleReturnValue("testView", createMethodParam("viewName"), mavContainer, webRequest);
		assertEquals("testView", mavContainer.getViewName());
	}

	private MethodParameter createMethodParam(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}
	
	@SuppressWarnings("unused")
	private View view() {
		return null;
	}

	@SuppressWarnings("unused")
	private String viewName() {
		return null;
	}
	
}

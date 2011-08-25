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
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Test fixture with {@link DefaultMethodReturnValueHandler}.
 * 
 * @author Rossen Stoyanchev
 */
public class ViewMethodReturnValueHandlerTests {

	private ViewMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	@Before
	public void setUp() {
		handler = new ViewMethodReturnValueHandler();
		mavContainer = new ModelAndViewContainer();
		webRequest = new ServletWebRequest(new MockHttpServletRequest());
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
	public void returnViewRedirect() throws Exception {
		RedirectView redirectView = new RedirectView("testView");
		ModelMap redirectModel = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectModel);
		handler.handleReturnValue(redirectView, createMethodParam("view"), mavContainer, webRequest);
		
		assertSame(redirectView, mavContainer.getView());
		assertSame("Should have switched to the RedirectModel", redirectModel, mavContainer.getModel());
	}
	
	@Test
	public void returnViewName() throws Exception {
		handler.handleReturnValue("testView", createMethodParam("viewName"), mavContainer, webRequest);
		
		assertEquals("testView", mavContainer.getViewName());
	}

	@Test
	public void returnViewNameRedirect() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectModel);
		handler.handleReturnValue("redirect:testView", createMethodParam("viewName"), mavContainer, webRequest);

		assertEquals("redirect:testView", mavContainer.getViewName());
		assertSame("Should have switched to the RedirectModel", redirectModel, mavContainer.getModel());
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
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ViewNameMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/**
 * Test fixture with {@link ViewNameMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewNameMethodReturnValueHandlerTests {

	private ViewNameMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	@Before
	public void setUp() {
		this.handler = new ViewNameMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(createReturnValueParam("viewName")));
	}

	@Test
	public void returnViewName() throws Exception {
		MethodParameter param = createReturnValueParam("viewName");
		this.handler.handleReturnValue("testView", param, this.mavContainer, this.webRequest);

		assertEquals("testView", this.mavContainer.getViewName());
	}

	@Test
	public void returnViewNameRedirect() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		MethodParameter param = createReturnValueParam("viewName");
		this.handler.handleReturnValue("redirect:testView", param, this.mavContainer, this.webRequest);

		assertEquals("redirect:testView", this.mavContainer.getViewName());
		assertSame("Should have switched to the RedirectModel", redirectModel, this.mavContainer.getModel());
	}

	private MethodParameter createReturnValueParam(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}

	String viewName() {
		return null;
	}

}
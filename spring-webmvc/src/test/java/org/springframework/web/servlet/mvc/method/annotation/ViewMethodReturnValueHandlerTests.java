/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link ViewMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewMethodReturnValueHandlerTests {

	private ViewMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;


	@Before
	public void setup() {
		this.handler = new ViewMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(createReturnValueParam("view")));
	}

	@Test
	public void returnView() throws Exception {
		InternalResourceView view = new InternalResourceView("testView");
		this.handler.handleReturnValue(view, createReturnValueParam("view"), this.mavContainer, this.webRequest);

		assertSame(view, this.mavContainer.getView());
	}

	@Test
	public void returnViewRedirect() throws Exception {
		RedirectView redirectView = new RedirectView("testView");
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		MethodParameter param = createReturnValueParam("view");
		this.handler.handleReturnValue(redirectView, param, this.mavContainer, this.webRequest);

		assertSame(redirectView, this.mavContainer.getView());
		assertSame("Should have switched to the RedirectModel", redirectModel, this.mavContainer.getModel());
	}

	private MethodParameter createReturnValueParam(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}


	View view() {
		return null;
	}

}

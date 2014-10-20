/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link ViewNameMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewNameMethodReturnValueHandlerTests {

	private ViewNameMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MethodParameter param;


	@Before
	public void setUp() throws NoSuchMethodException {
		this.handler = new ViewNameMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
		this.param = new MethodParameter(getClass().getDeclaredMethod("viewName"), -1);
	}

	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(this.handler.supportsReturnType(this.param));
	}

	@Test
	public void returnViewName() throws Exception {
		this.handler.handleReturnValue("testView", this.param, this.mavContainer, this.webRequest);
		assertEquals("testView", this.mavContainer.getViewName());
	}

	@Test
	public void returnViewNameRedirect() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		this.handler.handleReturnValue("redirect:testView", this.param, this.mavContainer, this.webRequest);
		assertEquals("redirect:testView", this.mavContainer.getViewName());
		assertSame(redirectModel, this.mavContainer.getModel());
	}

	@Test
	public void returnViewCustomRedirect() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		this.handler.setRedirectPatterns("myRedirect:*");
		this.handler.handleReturnValue("myRedirect:testView", this.param, this.mavContainer, this.webRequest);
		assertEquals("myRedirect:testView", this.mavContainer.getViewName());
		assertSame(redirectModel, this.mavContainer.getModel());
	}

	@Test
	public void returnViewRedirectWithCustomRedirectPattern() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		this.handler.setRedirectPatterns("myRedirect:*");
		this.handler.handleReturnValue("redirect:testView", this.param, this.mavContainer, this.webRequest);
		assertEquals("redirect:testView", this.mavContainer.getViewName());
		assertSame(redirectModel, this.mavContainer.getModel());
	}


	@SuppressWarnings("unused")
	String viewName() {
		return null;
	}

}
/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link ModelAndViewMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAndViewMethodReturnValueHandlerTests {

	private ModelAndViewMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MethodParameter returnParamModelAndView;


	@Before
	public void setUp() throws Exception {
		this.handler = new ModelAndViewMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
		this.returnParamModelAndView = getReturnValueParam("modelAndView");
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(handler.supportsReturnType(returnParamModelAndView));
		assertFalse(handler.supportsReturnType(getReturnValueParam("viewName")));
	}

	@Test
	public void handleViewReference() throws Exception {
		ModelAndView mav = new ModelAndView("viewName", "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertEquals("viewName", mavContainer.getView());
		assertEquals("attrValue", mavContainer.getModel().get("attrName"));
	}

	@Test
	public void handleViewInstance() throws Exception {
		ModelAndView mav = new ModelAndView(new RedirectView(), "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertEquals(RedirectView.class, mavContainer.getView().getClass());
		assertEquals("attrValue", mavContainer.getModel().get("attrName"));
	}

	@Test
	public void handleNull() throws Exception {
		handler.handleReturnValue(null, returnParamModelAndView, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
	}

	@Test
	public void handleRedirectAttributesWithViewReference() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView(new RedirectView(), "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertEquals(RedirectView.class, mavContainer.getView().getClass());
		assertEquals("attrValue", mavContainer.getModel().get("attrName"));
		assertSame("RedirectAttributes should be used if controller redirects", redirectAttributes,
				mavContainer.getModel());
	}

	@Test
	public void handleRedirectAttributesWithViewName() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView("redirect:viewName", "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertEquals("redirect:viewName", mavContainer.getViewName());
		assertEquals("attrValue", model.get("attrName"));
		assertSame(redirectAttributes, model);
	}

	@Test
	public void handleRedirectAttributesWithCustomPrefix() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView("myRedirect:viewName", "attrName", "attrValue");
		handler.setRedirectPatterns("myRedirect:*");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertEquals("myRedirect:viewName", mavContainer.getViewName());
		assertEquals("attrValue", model.get("attrName"));
		assertSame(redirectAttributes, model);
	}

	@Test
	public void handleRedirectAttributesWithoutRedirect() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView();
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertEquals(null, mavContainer.getView());
		assertTrue(mavContainer.getModel().isEmpty());
		assertNotSame("RedirectAttributes should not be used if controller doesn't redirect", redirectAttributes, model);
	}

	@Test  // SPR-14045
	public void handleRedirectWithIgnoreDefaultModel() throws Exception {
		mavContainer.setIgnoreDefaultModelOnRedirect(true);

		RedirectView redirectView = new RedirectView();
		ModelAndView mav = new ModelAndView(redirectView, "name", "value");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertSame(redirectView, mavContainer.getView());
		assertEquals(1, model.size());
		assertEquals("value", model.get("name"));
	}


	private MethodParameter getReturnValueParam(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}


	@SuppressWarnings("unused")
	ModelAndView modelAndView() {
		return null;
	}

	@SuppressWarnings("unused")
	String viewName() {
		return null;
	}

}

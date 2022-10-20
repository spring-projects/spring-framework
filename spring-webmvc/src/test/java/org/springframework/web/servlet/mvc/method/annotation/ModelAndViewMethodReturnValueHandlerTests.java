/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

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


	@BeforeEach
	public void setup() throws Exception {
		this.handler = new ModelAndViewMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
		this.returnParamModelAndView = getReturnValueParam("modelAndView");
	}


	@Test
	public void supportsReturnType() throws Exception {
		assertThat(handler.supportsReturnType(returnParamModelAndView)).isTrue();
		assertThat(handler.supportsReturnType(getReturnValueParam("viewName"))).isFalse();
	}

	@Test
	public void handleViewReference() throws Exception {
		ModelAndView mav = new ModelAndView("viewName", "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.getView()).isEqualTo("viewName");
		assertThat(mavContainer.getModel().get("attrName")).isEqualTo("attrValue");
	}

	@Test
	public void handleViewInstance() throws Exception {
		ModelAndView mav = new ModelAndView(new RedirectView(), "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.getView().getClass()).isEqualTo(RedirectView.class);
		assertThat(mavContainer.getModel().get("attrName")).isEqualTo("attrValue");
	}

	@Test
	public void handleNull() throws Exception {
		handler.handleReturnValue(null, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
	}

	@Test
	public void handleRedirectAttributesWithViewReference() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView(new RedirectView(), "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.getView().getClass()).isEqualTo(RedirectView.class);
		assertThat(mavContainer.getModel().get("attrName")).isEqualTo("attrValue");
		assertThat(mavContainer.getModel()).as("RedirectAttributes should be used if controller redirects").isSameAs(redirectAttributes);
	}

	@Test
	public void handleRedirectAttributesWithViewName() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView("redirect:viewName", "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getViewName()).isEqualTo("redirect:viewName");
		assertThat(model.get("attrName")).isEqualTo("attrValue");
		assertThat(model).isSameAs(redirectAttributes);
	}

	@Test
	public void handleRedirectAttributesWithCustomPrefix() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView("myRedirect:viewName", "attrName", "attrValue");
		handler.setRedirectPatterns("myRedirect:*");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getViewName()).isEqualTo("myRedirect:viewName");
		assertThat(model.get("attrName")).isEqualTo("attrValue");
		assertThat(model).isSameAs(redirectAttributes);
	}

	@Test
	public void handleRedirectAttributesWithoutRedirect() throws Exception {
		RedirectAttributesModelMap redirectAttributes  = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView();
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getView()).isNull();
		assertThat(mavContainer.getModel().isEmpty()).isTrue();
		assertThat(model).as("RedirectAttributes should not be used if controller doesn't redirect").isNotSameAs(redirectAttributes);
	}

	@Test  // SPR-14045
	public void handleRedirectWithIgnoreDefaultModel() throws Exception {
		RedirectView redirectView = new RedirectView();
		ModelAndView mav = new ModelAndView(redirectView, "name", "value");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getView()).isSameAs(redirectView);
		assertThat(model.size()).isEqualTo(1);
		assertThat(model.get("name")).isEqualTo("value");
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

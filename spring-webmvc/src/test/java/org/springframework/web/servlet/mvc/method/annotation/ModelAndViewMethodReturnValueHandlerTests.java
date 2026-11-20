/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.FragmentsRendering;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ModelAndViewMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
class ModelAndViewMethodReturnValueHandlerTests {

	private ModelAndViewMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MethodParameter returnParamModelAndView;


	@BeforeEach
	void setup() throws Exception {
		this.handler = new ModelAndViewMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
		this.returnParamModelAndView = getReturnValueParam("modelAndView");
	}


	@Test
	void supportsReturnType() throws Exception {
		assertThat(handler.supportsReturnType(returnParamModelAndView)).isTrue();
		assertThat(handler.supportsReturnType(getReturnValueParam("fragmentsRendering"))).isTrue();
		assertThat(handler.supportsReturnType(getReturnValueParam("fragmentsCollection"))).isTrue();

		assertThat(handler.supportsReturnType(getReturnValueParam("viewName"))).isFalse();
	}

	@Test
	void handleViewReference() throws Exception {
		ModelAndView mav = new ModelAndView("viewName", "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.getView()).isEqualTo("viewName");
		assertThat(mavContainer.getModel().get("attrName")).isEqualTo("attrValue");
	}

	@Test
	void handleViewInstance() throws Exception {
		ModelAndView mav = new ModelAndView(new RedirectView(), "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.getView().getClass()).isEqualTo(RedirectView.class);
		assertThat(mavContainer.getModel().get("attrName")).isEqualTo("attrValue");
	}

	@Test
	void handleFragmentsRendering() throws Exception {
		FragmentsRendering rendering = FragmentsRendering.fragment("viewName").build();

		handler.handleReturnValue(rendering, returnParamModelAndView, mavContainer, webRequest);
		assertThat(mavContainer.getView()).isInstanceOf(SmartView.class);
	}

	@Test
	void handleFragmentsCollection() throws Exception {
		Collection<ModelAndView> fragments = List.of(new ModelAndView("viewName"));

		handler.handleReturnValue(fragments, returnParamModelAndView, mavContainer, webRequest);
		assertThat(mavContainer.getView()).isInstanceOf(SmartView.class);
	}

	@Test
	void handleNull() throws Exception {
		handler.handleReturnValue(null, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
	}

	@Test
	void handleRedirectAttributesWithViewReference() throws Exception {
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView(new RedirectView(), "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		assertThat(mavContainer.getView().getClass()).isEqualTo(RedirectView.class);
		assertThat(mavContainer.getModel().get("attrName")).isEqualTo("attrValue");
		assertThat(mavContainer.getModel()).as("RedirectAttributes should be used if controller redirects").isSameAs(redirectAttributes);
	}

	@Test
	void handleRedirectAttributesWithViewName() throws Exception {
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView("redirect:viewName", "attrName", "attrValue");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getViewName()).isEqualTo("redirect:viewName");
		assertThat(model.get("attrName")).isEqualTo("attrValue");
		assertThat(model).isSameAs(redirectAttributes);
	}

	@Test
	void handleRedirectAttributesWithCustomPrefix() throws Exception {
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
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
	void handleRedirectAttributesWithoutRedirect() throws Exception {
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
		mavContainer.setRedirectModel(redirectAttributes);

		ModelAndView mav = new ModelAndView();
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getView()).isNull();
		assertThat(mavContainer.getModel()).isEmpty();
		assertThat(model).as("RedirectAttributes should not be used if controller doesn't redirect").isNotSameAs(redirectAttributes);
	}

	@Test  // SPR-14045
	void handleRedirectWithIgnoreDefaultModel() throws Exception {
		RedirectView redirectView = new RedirectView();
		ModelAndView mav = new ModelAndView(redirectView, "name", "value");
		handler.handleReturnValue(mav, returnParamModelAndView, mavContainer, webRequest);

		ModelMap model = mavContainer.getModel();
		assertThat(mavContainer.getView()).isSameAs(redirectView);
		assertThat(model).hasSize(1);
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

	@SuppressWarnings("unused")
	FragmentsRendering fragmentsRendering() {
		return null;
	}

	@SuppressWarnings("unused")
	Collection<ModelAndView> fragmentsCollection() {
		return null;
	}

}

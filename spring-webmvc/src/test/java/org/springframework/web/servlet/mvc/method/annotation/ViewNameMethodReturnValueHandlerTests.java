/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ViewNameMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
class ViewNameMethodReturnValueHandlerTests {

	private ViewNameMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MethodParameter param;


	@BeforeEach
	void setup() throws NoSuchMethodException {
		this.handler = new ViewNameMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		this.param = new MethodParameter(getClass().getDeclaredMethod("viewName"), -1);
	}


	@Test
	void supportsReturnType() throws Exception {
		assertThat(this.handler.supportsReturnType(this.param)).isTrue();
	}

	@Test
	void returnViewName() throws Exception {
		this.handler.handleReturnValue("testView", this.param, this.mavContainer, this.webRequest);
		assertThat(this.mavContainer.getViewName()).isEqualTo("testView");
	}

	@Test
	void returnViewNameRedirect() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		this.handler.handleReturnValue("redirect:testView", this.param, this.mavContainer, this.webRequest);
		assertThat(this.mavContainer.getViewName()).isEqualTo("redirect:testView");
		assertThat(this.mavContainer.getModel()).isSameAs(redirectModel);
	}

	@Test
	void returnViewCustomRedirect() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		this.handler.setRedirectPatterns("myRedirect:*");
		this.handler.handleReturnValue("myRedirect:testView", this.param, this.mavContainer, this.webRequest);
		assertThat(this.mavContainer.getViewName()).isEqualTo("myRedirect:testView");
		assertThat(this.mavContainer.getModel()).isSameAs(redirectModel);
	}

	@Test
	void returnViewRedirectWithCustomRedirectPattern() throws Exception {
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		this.handler.setRedirectPatterns("myRedirect:*");
		this.handler.handleReturnValue("redirect:testView", this.param, this.mavContainer, this.webRequest);
		assertThat(this.mavContainer.getViewName()).isEqualTo("redirect:testView");
		assertThat(this.mavContainer.getModel()).isSameAs(redirectModel);
	}


	@SuppressWarnings("unused")
	String viewName() {
		return null;
	}

}

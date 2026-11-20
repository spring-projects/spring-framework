/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link ViewMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
class ViewMethodReturnValueHandlerTests {

	private ViewMethodReturnValueHandler handler;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;


	@BeforeEach
	void setup() {
		this.handler = new ViewMethodReturnValueHandler();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	void supportsReturnType() throws Exception {
		assertThat(this.handler.supportsReturnType(createReturnValueParam("view"))).isTrue();
	}

	@Test
	void returnView() throws Exception {
		InternalResourceView view = new InternalResourceView("testView");
		this.handler.handleReturnValue(view, createReturnValueParam("view"), this.mavContainer, this.webRequest);

		assertThat(this.mavContainer.getView()).isSameAs(view);
	}

	@Test
	void returnViewRedirect() throws Exception {
		RedirectView redirectView = new RedirectView("testView");
		ModelMap redirectModel = new RedirectAttributesModelMap();
		this.mavContainer.setRedirectModel(redirectModel);
		MethodParameter param = createReturnValueParam("view");
		this.handler.handleReturnValue(redirectView, param, this.mavContainer, this.webRequest);

		assertThat(this.mavContainer.getView()).isSameAs(redirectView);
		assertThat(this.mavContainer.getModel()).as("Should have switched to the RedirectModel").isSameAs(redirectModel);
	}

	private MethodParameter createReturnValueParam(String methodName) throws Exception {
		Method method = getClass().getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}


	View view() {
		return null;
	}

}

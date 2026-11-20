/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.ModelMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
class ModelMethodProcessorTests {

	private ModelMethodProcessor processor;

	private ModelAndViewContainer mavContainer;

	private MethodParameter paramModel;

	private MethodParameter returnParamModel;

	private NativeWebRequest webRequest;

	@BeforeEach
	void setUp() throws Exception {
		processor = new ModelMethodProcessor();
		mavContainer = new ModelAndViewContainer();

		Method method = getClass().getDeclaredMethod("model", Model.class);
		paramModel = new MethodParameter(method, 0);
		returnParamModel = new MethodParameter(method, -1);

		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	void supportsParameter() {
		assertThat(processor.supportsParameter(paramModel)).isTrue();
	}

	@Test
	void supportsReturnType() {
		assertThat(processor.supportsReturnType(returnParamModel)).isTrue();
	}

	@Test
	void resolveArgumentValue() throws Exception {
		assertThat(processor.resolveArgument(paramModel, mavContainer, webRequest, null)).isSameAs(mavContainer.getModel());
	}

	@Test
	void handleModelReturnValue() throws Exception {
		mavContainer.addAttribute("attr1", "value1");
		Model returnValue = new ExtendedModelMap();
		returnValue.addAttribute("attr2", "value2");

		processor.handleReturnValue(returnValue , returnParamModel, mavContainer, webRequest);

		assertThat(mavContainer.getModel().get("attr1")).isEqualTo("value1");
		assertThat(mavContainer.getModel().get("attr2")).isEqualTo("value2");
	}

	@SuppressWarnings("unused")
	private Model model(Model model) {
		return null;
	}

}

/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.ModelMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelMethodProcessorTests {

	private ModelMethodProcessor processor;

	private ModelAndViewContainer mavContainer;

	private MethodParameter paramModel;

	private MethodParameter returnParamModel;

	private NativeWebRequest webRequest;

	@BeforeEach
	public void setUp() throws Exception {
		processor = new ModelMethodProcessor();
		mavContainer = new ModelAndViewContainer();

		Method method = getClass().getDeclaredMethod("model", Model.class);
		paramModel = new MethodParameter(method, 0);
		returnParamModel = new MethodParameter(method, -1);

		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportsParameter() {
		assertThat(processor.supportsParameter(paramModel)).isTrue();
	}

	@Test
	public void supportsReturnType() {
		assertThat(processor.supportsReturnType(returnParamModel)).isTrue();
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		assertThat(processor.resolveArgument(paramModel, mavContainer, webRequest, null)).isSameAs(mavContainer.getModel());
	}

	@Test
	public void handleModelReturnValue() throws Exception {
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

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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ServletModelAttributeMethodProcessor} specific tests.
 * Also see org.springframework.web.method.annotation.support.ModelAttributeMethodProcessorTests
 *
 * @author Rossen Stoyanchev
 */
public class ServletModelAttributeMethodProcessorTests {

	private ServletModelAttributeMethodProcessor processor;

	private WebDataBinderFactory binderFactory;

	private ModelAndViewContainer mavContainer;

	private MockHttpServletRequest request;

	private NativeWebRequest webRequest;

	private MethodParameter testBeanModelAttr;
	private MethodParameter testBeanWithoutStringConstructorModelAttr;
	private MethodParameter testBeanWithOptionalModelAttr;


	@BeforeEach
	public void setup() throws Exception {
		processor = new ServletModelAttributeMethodProcessor(false);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		binderFactory = new ServletRequestDataBinderFactory(null, initializer);

		mavContainer = new ModelAndViewContainer();
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request);

		Method method = getClass().getDeclaredMethod("modelAttribute",
				TestBean.class, TestBeanWithoutStringConstructor.class, Optional.class);
		testBeanModelAttr = new MethodParameter(method, 0);
		testBeanWithoutStringConstructorModelAttr = new MethodParameter(method, 1);
		testBeanWithOptionalModelAttr = new MethodParameter(method, 2);
	}


	@Test
	public void createAttributeUriTemplateVar() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("testBean1", "Patty");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		// Type conversion from "Patty" to TestBean via TestBean(String) constructor
		TestBean testBean = (TestBean) processor.resolveArgument(
				testBeanModelAttr, mavContainer, webRequest, binderFactory);

		assertThat(testBean.getName()).isEqualTo("Patty");
	}

	@Test
	public void createAttributeUriTemplateVarCannotConvert() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("testBean2", "Patty");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		TestBeanWithoutStringConstructor testBean = (TestBeanWithoutStringConstructor) processor.resolveArgument(
				testBeanWithoutStringConstructorModelAttr, mavContainer, webRequest, binderFactory);

		assertThat(testBean).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createAttributeUriTemplateVarWithOptional() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("testBean3", "Patty");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		// Type conversion from "Patty" to TestBean via TestBean(String) constructor
		Optional<TestBean> testBean = (Optional<TestBean>) processor.resolveArgument(
				testBeanWithOptionalModelAttr, mavContainer, webRequest, binderFactory);

		assertThat(testBean.get().getName()).isEqualTo("Patty");
	}

	@Test
	public void createAttributeRequestParameter() throws Exception {
		request.addParameter("testBean1", "Patty");

		// Type conversion from "Patty" to TestBean via TestBean(String) constructor
		TestBean testBean = (TestBean) processor.resolveArgument(
				testBeanModelAttr, mavContainer, webRequest, binderFactory);

		assertThat(testBean.getName()).isEqualTo("Patty");
	}

	@Test
	public void createAttributeRequestParameterCannotConvert() throws Exception {
		request.addParameter("testBean2", "Patty");

		TestBeanWithoutStringConstructor testBean = (TestBeanWithoutStringConstructor) processor.resolveArgument(
				testBeanWithoutStringConstructorModelAttr, mavContainer, webRequest, binderFactory);

		assertThat(testBean).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void createAttributeRequestParameterWithOptional() throws Exception {
		request.addParameter("testBean3", "Patty");

		Optional<TestBean> testBean = (Optional<TestBean>) processor.resolveArgument(
				testBeanWithOptionalModelAttr, mavContainer, webRequest, binderFactory);

		assertThat(testBean.get().getName()).isEqualTo("Patty");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void attributesAsNullValues() throws Exception {
		request.addParameter("name", "Patty");

		mavContainer.getModel().put("testBean1", null);
		mavContainer.getModel().put("testBean2", null);
		mavContainer.getModel().put("testBean3", null);

		assertThat(processor.resolveArgument(
				testBeanModelAttr, mavContainer, webRequest, binderFactory)).isNull();

		assertThat(processor.resolveArgument(
				testBeanWithoutStringConstructorModelAttr, mavContainer, webRequest, binderFactory)).isNull();

		Optional<TestBean> testBean = (Optional<TestBean>) processor.resolveArgument(
				testBeanWithOptionalModelAttr, mavContainer, webRequest, binderFactory);
		assertThat(testBean.isPresent()).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void attributesAsOptionalEmpty() throws Exception {
		request.addParameter("name", "Patty");

		mavContainer.getModel().put("testBean1", Optional.empty());
		mavContainer.getModel().put("testBean2", Optional.empty());
		mavContainer.getModel().put("testBean3", Optional.empty());

		assertThat(processor.resolveArgument(
				testBeanModelAttr, mavContainer, webRequest, binderFactory)).isNull();

		assertThat(processor.resolveArgument(
				testBeanWithoutStringConstructorModelAttr, mavContainer, webRequest, binderFactory)).isNull();

		Optional<TestBean> testBean =(Optional<TestBean>) processor.resolveArgument(
				testBeanWithOptionalModelAttr, mavContainer, webRequest, binderFactory);
		assertThat(testBean.isPresent()).isFalse();
	}


	@SuppressWarnings("unused")
	private void modelAttribute(@ModelAttribute("testBean1") TestBean testBean1,
								@ModelAttribute("testBean2") TestBeanWithoutStringConstructor testBean2,
								@ModelAttribute("testBean3") Optional<TestBean> testBean3) {
	}


	@SuppressWarnings("unused")
	private static class TestBeanWithoutStringConstructor {

		public TestBeanWithoutStringConstructor() {
		}

		public TestBeanWithoutStringConstructor(int i) {
		}
	}

}

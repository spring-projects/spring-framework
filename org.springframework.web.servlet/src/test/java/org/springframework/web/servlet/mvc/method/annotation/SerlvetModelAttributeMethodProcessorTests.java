/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletModelAttributeMethodProcessor;

/**
 * Test fixture for {@link ServletModelAttributeMethodProcessor} specific tests.
 * Also see org.springframework.web.method.annotation.support.ModelAttributeMethodProcessorTests
 * 
 * @author Rossen Stoyanchev
 */
public class SerlvetModelAttributeMethodProcessorTests {

	private ServletModelAttributeMethodProcessor processor;

	private MethodParameter testBeanModelAttr;
	
	private MethodParameter testBeanWithoutStringConstructorModelAttr;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	private WebDataBinderFactory binderFactory;

	@Before
	public void setUp() throws Exception {
		processor = new ServletModelAttributeMethodProcessor(false);

		Method method = getClass().getDeclaredMethod("modelAttribute", 
				TestBean.class, TestBeanWithoutStringConstructor.class);
		
		testBeanModelAttr = new MethodParameter(method, 0);
		testBeanWithoutStringConstructorModelAttr = new MethodParameter(method, 1);
		
		binderFactory = new ServletRequestDataBinderFactory(null, null);
		mavContainer = new ModelAndViewContainer();
		
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request);
	}

	@Test
	public void createAttributeViaPathVariable() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<String, String>();
		uriTemplateVars.put("testBean1", "pathy");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		// Type conversion from "pathy" to TestBean via TestBean(String) constructor

		TestBean testBean = 
			(TestBean) processor.resolveArgument(testBeanModelAttr, mavContainer, webRequest, binderFactory);
		
		assertEquals("pathy", testBean.getName());
	}

	@Test
	public void createAttributeAfterPathVariableConversionError() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<String, String>();
		uriTemplateVars.put("testBean1", "pathy");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		TestBeanWithoutStringConstructor testBean = 
			(TestBeanWithoutStringConstructor) processor.resolveArgument(
				testBeanWithoutStringConstructorModelAttr, mavContainer, webRequest, binderFactory);
		
		assertNotNull(testBean);
	}

	
	@SuppressWarnings("unused")
	private void modelAttribute(@ModelAttribute("testBean1") TestBean testBean1, 
								@ModelAttribute("testBean2") TestBeanWithoutStringConstructor testBean2) { 
	}


	@SuppressWarnings("unused")
	private static class TestBeanWithoutStringConstructor {

		public TestBeanWithoutStringConstructor() {
		}
		
		public TestBeanWithoutStringConstructor(int i) {
		}
		
	}

}

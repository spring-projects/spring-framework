/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.ModelAndViewResolverMethodReturnValueHandler;

/**
 * Test fixture with {@link ModelAndViewResolverMethodReturnValueHandler}.
 * 
 * @author Rossen Stoyanchev
 */
public class ModelAndViewResolverMethodReturnValueHandlerTests {

	private ModelAndViewResolverMethodReturnValueHandler handler;

	private List<ModelAndViewResolver> mavResolvers;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest request;

	@Before
	public void setUp() {
		mavResolvers = new ArrayList<ModelAndViewResolver>();
		handler = new ModelAndViewResolverMethodReturnValueHandler(mavResolvers);
		mavContainer = new ModelAndViewContainer();
		request = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void modelAndViewResolver() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("testBeanReturnValue"), -1);
		mavResolvers.add(new TestModelAndViewResolver(TestBean.class));
		TestBean testBean = new TestBean("name");

		handler.handleReturnValue(testBean, returnType, mavContainer, request);
		
		assertEquals("viewName", mavContainer.getViewName());
		assertSame(testBean, mavContainer.getModel().get("modelAttrName"));
		assertFalse(mavContainer.isRequestHandled());
	}

	@Test(expected=UnsupportedOperationException.class)
	public void modelAndViewResolverUnresolved() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("intReturnValue"), -1);
		mavResolvers.add(new TestModelAndViewResolver(TestBean.class));
		handler.handleReturnValue(99, returnType, mavContainer, request);
	}

	@Test
	public void handleNull() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("testBeanReturnValue"), -1);
		handler.handleReturnValue(null, returnType, mavContainer, request);
		
		assertNull(mavContainer.getView());
		assertNull(mavContainer.getViewName());
		assertTrue(mavContainer.getModel().isEmpty());
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void handleSimpleType() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("intReturnValue"), -1);
		handler.handleReturnValue(55, returnType, mavContainer, request);
	}

	@Test
	public void handleNonSimpleType() throws Exception{
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("testBeanReturnValue"), -1);
		handler.handleReturnValue(new TestBean(), returnType, mavContainer, request);
		
		assertTrue(mavContainer.containsAttribute("testBean"));
	}

	@SuppressWarnings("unused")
	private int intReturnValue() {
		return 0;
	}

	@SuppressWarnings("unused")
	private TestBean testBeanReturnValue() {
		return null;
	}
	
	private static class TestModelAndViewResolver implements ModelAndViewResolver {
		
		private Class<?> returnValueType;
		
		public TestModelAndViewResolver(Class<?> returnValueType) {
			this.returnValueType = returnValueType;
		}

		@SuppressWarnings("rawtypes")
		public ModelAndView resolveModelAndView(Method method, Class handlerType, Object returnValue,
				ExtendedModelMap model, NativeWebRequest request) {
			if (returnValue != null && returnValue.getClass().equals(returnValueType)) {
				return new ModelAndView("viewName", "modelAttrName", returnValue);
			}
			else {
				return ModelAndViewResolver.UNRESOLVED;
			}
		}
	}
}
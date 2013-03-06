/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Text fixture for {@link ModelFactory} tests.
 *
 * @author Rossen Stoyanchev
 */
public class ModelFactoryTests {

	private Object handler = new ModelHandler();

	private InvocableHandlerMethod handleMethod;

	private InvocableHandlerMethod handleSessionAttrMethod;

	private SessionAttributesHandler sessionAttrsHandler;

	private SessionAttributeStore sessionAttributeStore;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		Class<?> handlerType = handler.getClass();
		handleMethod = new InvocableHandlerMethod(handler, handlerType.getDeclaredMethod("handle"));
		Method method = handlerType.getDeclaredMethod("handleSessionAttr", String.class);
		handleSessionAttrMethod = new InvocableHandlerMethod(handler, method);
		sessionAttributeStore = new DefaultSessionAttributeStore();
		sessionAttrsHandler = new SessionAttributesHandler(handlerType, sessionAttributeStore);
		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void modelAttributeMethod() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getModel().get("modelAttr"));
	}

	@Test
	public void modelAttributeMethodWithSpecifiedName() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrWithName");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getModel().get("name"));
	}

	@Test
	public void modelAttributeMethodWithNameByConvention() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrConvention");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getModel().get("boolean"));
	}

	@Test
	public void modelAttributeMethodWithNullReturnValue() throws Exception {
		ModelFactory modelFactory = createModelFactory("nullModelAttr");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertTrue(mavContainer.containsAttribute("name"));
		assertNull(mavContainer.getModel().get("name"));
	}

	@Test
	public void sessionAttribute() throws Exception {
		sessionAttributeStore.storeAttribute(webRequest, "sessionAttr", "sessionAttrValue");

		// Resolve successfully handler session attribute once
		assertTrue(sessionAttrsHandler.isHandlerSessionAttribute("sessionAttr", null));

		ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals("sessionAttrValue", mavContainer.getModel().get("sessionAttr"));
	}

	@Test
	public void requiredSessionAttribute() throws Exception {
		ModelFactory modelFactory = new ModelFactory(null, null, sessionAttrsHandler);

		try {
			modelFactory.initModel(webRequest, new ModelAndViewContainer(), handleSessionAttrMethod);
			fail("Expected HttpSessionRequiredException");
		} catch (HttpSessionRequiredException e) { }

		sessionAttributeStore.storeAttribute(webRequest, "sessionAttr", "sessionAttrValue");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(webRequest, mavContainer, handleSessionAttrMethod);

		assertEquals("sessionAttrValue", mavContainer.getModel().get("sessionAttr"));
	}

	@Test
	public void updateModelBindingResultKeys() throws Exception {
		String attrName = "attr1";
		Object attrValue = new Object();
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAttribute(attrName, attrValue);

		WebDataBinder dataBinder = new WebDataBinder(attrValue, attrName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(webRequest, attrValue, attrName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, sessionAttrsHandler);
		modelFactory.updateModel(webRequest, mavContainer);

		assertEquals(attrValue, mavContainer.getModel().remove(attrName));
		assertSame(dataBinder.getBindingResult(), mavContainer.getModel().remove(bindingResultKey(attrName)));
		assertEquals(0, mavContainer.getModel().size());
	}

	@Test
	public void updateModelSessionStatusComplete() throws Exception {
		String attrName = "sessionAttr";
		String attrValue = "sessionAttrValue";

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAttribute(attrName, attrValue);
		mavContainer.getSessionStatus().setComplete();
		sessionAttributeStore.storeAttribute(webRequest, attrName, attrValue);

		// Resolve successfully handler session attribute once
		assertTrue(sessionAttrsHandler.isHandlerSessionAttribute(attrName, null));

		WebDataBinder dataBinder = new WebDataBinder(attrValue, attrName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(webRequest, attrValue, attrName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, sessionAttrsHandler);
		modelFactory.updateModel(webRequest, mavContainer);

		assertEquals(attrValue, mavContainer.getModel().get(attrName));
		assertNull(sessionAttributeStore.retrieveAttribute(webRequest, attrName));
	}

	private String bindingResultKey(String key) {
		return BindingResult.MODEL_KEY_PREFIX + key;
	}

	private ModelFactory createModelFactory(String methodName, Class<?>... parameterTypes) throws Exception{
		Method method = ModelHandler.class.getMethod(methodName, parameterTypes);

		HandlerMethodArgumentResolverComposite argResolvers = new HandlerMethodArgumentResolverComposite();
		argResolvers.addResolver(new ModelMethodProcessor());

		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(handler, method);
		handlerMethod.setHandlerMethodArgumentResolvers(argResolvers);
		handlerMethod.setDataBinderFactory(null);
		handlerMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());

		return new ModelFactory(Arrays.asList(handlerMethod), null, sessionAttrsHandler);
	}

	@SessionAttributes("sessionAttr") @SuppressWarnings("unused")
	private static class ModelHandler {

		@ModelAttribute
		public void modelAttr(Model model) {
			model.addAttribute("modelAttr", Boolean.TRUE);
		}

		@ModelAttribute("name")
		public Boolean modelAttrWithName() {
			return Boolean.TRUE;
		}

		@ModelAttribute
		public Boolean modelAttrConvention() {
			return Boolean.TRUE;
		}

		@ModelAttribute("name")
		public Boolean nullModelAttr() {
			return null;
		}

		public void handle() {
		}

		public void handleSessionAttr(@ModelAttribute("sessionAttr") String sessionAttr) {
		}
	}
}

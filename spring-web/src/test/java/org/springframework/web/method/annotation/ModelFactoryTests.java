/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
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


/**
 * Text fixture for {@link ModelFactory} tests.
 *
 * @author Rossen Stoyanchev
 */
public class ModelFactoryTests {

	private TestController controller = new TestController();

	private InvocableHandlerMethod handleMethod;

	private InvocableHandlerMethod handleSessionAttrMethod;

	private SessionAttributesHandler sessionAttrsHandler;

	private SessionAttributeStore sessionAttributeStore;

	private NativeWebRequest webRequest;


	@Before
	public void setUp() throws Exception {
		this.controller = new TestController();

		Method method = TestController.class.getDeclaredMethod("handle");
		this.handleMethod = new InvocableHandlerMethod(this.controller, method);

		method = TestController.class.getDeclaredMethod("handleSessionAttr", String.class);
		this.handleSessionAttrMethod = new InvocableHandlerMethod(this.controller, method);

		this.sessionAttributeStore = new DefaultSessionAttributeStore();
		this.sessionAttrsHandler = new SessionAttributesHandler(TestController.class, this.sessionAttributeStore);
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	public void modelAttributeMethod() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(this.webRequest, mavContainer, this.handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getModel().get("modelAttr"));
	}

	@Test
	public void modelAttributeMethodWithExplicitName() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrWithName");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(this.webRequest, mavContainer, this.handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getModel().get("name"));
	}

	@Test
	public void modelAttributeMethodWithNameByConvention() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrConvention");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(this.webRequest, mavContainer, this.handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getModel().get("boolean"));
	}

	@Test
	public void modelAttributeMethodWithNullReturnValue() throws Exception {
		ModelFactory modelFactory = createModelFactory("nullModelAttr");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(this.webRequest, mavContainer, this.handleMethod);

		assertTrue(mavContainer.containsAttribute("name"));
		assertNull(mavContainer.getModel().get("name"));
	}

	@Test
	public void sessionAttribute() throws Exception {
		this.sessionAttributeStore.storeAttribute(this.webRequest, "sessionAttr", "sessionAttrValue");

		// Resolve successfully handler session attribute once
		assertTrue(sessionAttrsHandler.isHandlerSessionAttribute("sessionAttr", null));

		ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(this.webRequest, mavContainer, this.handleMethod);

		assertEquals("sessionAttrValue", mavContainer.getModel().get("sessionAttr"));
	}

	@Test
	public void sessionAttributeNotPresent() throws Exception {
		ModelFactory modelFactory = new ModelFactory(null, null, this.sessionAttrsHandler);

		try {
			modelFactory.initModel(this.webRequest, new ModelAndViewContainer(), this.handleSessionAttrMethod);
			fail("Expected HttpSessionRequiredException");
		}
		catch (HttpSessionRequiredException e) {
			// expected
		}

		this.sessionAttributeStore.storeAttribute(this.webRequest, "sessionAttr", "sessionAttrValue");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		modelFactory.initModel(this.webRequest, mavContainer, this.handleSessionAttrMethod);

		assertEquals("sessionAttrValue", mavContainer.getModel().get("sessionAttr"));
	}

	@Test
	public void updateModelBindingResult() throws Exception {
		String commandName = "attr1";
		Object command = new Object();
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(commandName, command);

		WebDataBinder dataBinder = new WebDataBinder(command, commandName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, command, commandName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.sessionAttrsHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertEquals(command, container.getModel().get(commandName));
		assertSame(dataBinder.getBindingResult(), container.getModel().get(bindingResultKey(commandName)));
		assertEquals(2, container.getModel().size());
	}

	@Test
	public void updateModelSessionAttributesSaved() throws Exception {
		String attributeName = "sessionAttr";
		String attribute = "value";
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(attributeName, attribute);

		WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.sessionAttrsHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertEquals(attribute, container.getModel().get(attributeName));
		assertEquals(attribute, this.sessionAttributeStore.retrieveAttribute(this.webRequest, attributeName));
	}

	@Test
	public void updateModelSessionAttributesRemoved() throws Exception {
		String attributeName = "sessionAttr";
		String attribute = "value";
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(attributeName, attribute);

		// Store and resolve once (to be "remembered")
		this.sessionAttributeStore.storeAttribute(this.webRequest, attributeName, attribute);
		this.sessionAttrsHandler.isHandlerSessionAttribute(attributeName, null);

		WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

		container.getSessionStatus().setComplete();

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.sessionAttrsHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertEquals(attribute, container.getModel().get(attributeName));
		assertNull(this.sessionAttributeStore.retrieveAttribute(this.webRequest, attributeName));
	}

	// SPR-12542

	@Test
	public void updateModelWhenRedirecting() throws Exception {
		String attributeName = "sessionAttr";
		String attribute = "value";
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(attributeName, attribute);

		String queryParam = "123";
		String queryParamName = "q";
		container.setRedirectModel(new ModelMap(queryParamName, queryParam));
		container.setRedirectModelScenario(true);

		WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.sessionAttrsHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertEquals(queryParam, container.getModel().get(queryParamName));
		assertEquals(1, container.getModel().size());
		assertEquals(attribute, this.sessionAttributeStore.retrieveAttribute(this.webRequest, attributeName));
	}


	private String bindingResultKey(String key) {
		return BindingResult.MODEL_KEY_PREFIX + key;
	}

	private ModelFactory createModelFactory(String methodName, Class<?>... parameterTypes) throws Exception{
		Method method = TestController.class.getMethod(methodName, parameterTypes);

		HandlerMethodArgumentResolverComposite argResolvers = new HandlerMethodArgumentResolverComposite();
		argResolvers.addResolver(new ModelMethodProcessor());

		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(this.controller, method);
		handlerMethod.setHandlerMethodArgumentResolvers(argResolvers);
		handlerMethod.setDataBinderFactory(null);
		handlerMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());

		return new ModelFactory(Arrays.asList(handlerMethod), null, this.sessionAttrsHandler);
	}

	@SessionAttributes("sessionAttr") @SuppressWarnings("unused")
	private static class TestController {

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

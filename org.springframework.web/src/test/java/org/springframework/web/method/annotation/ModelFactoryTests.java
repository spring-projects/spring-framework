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

package org.springframework.web.method.annotation;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SimpleSessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.support.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Text fixture for {@link ModelFactory} tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ModelFactoryTests {
	
	private Object handler = new ModelHandler();
	
	private InvocableHandlerMethod handleMethod;

	private SessionAttributesHandler handlerSessionAttributeStore;
	
	private SessionAttributeStore sessionAttributeStore;

	private ModelAndViewContainer mavContainer;
	
	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		handleMethod = new InvocableHandlerMethod(handler, handler.getClass().getDeclaredMethod("handle"));
		sessionAttributeStore = new DefaultSessionAttributeStore();
		handlerSessionAttributeStore = new SessionAttributesHandler(handler.getClass(), sessionAttributeStore);
		mavContainer = new ModelAndViewContainer();
		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}
	
	@Test
	public void createModel() throws Exception {
		ModelFactory modelFactory = createModelFactory("model", Model.class);
		modelFactory.initModel(webRequest, mavContainer, handleMethod);
		
		assertEquals(Boolean.TRUE, mavContainer.getAttribute("model"));
	}

	@Test
	public void createModelWithName() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelWithName");
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getAttribute("name"));
	}

	@Test
	public void createModelWithDefaultName() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelWithDefaultName");
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getAttribute("boolean"));
	}

	@Test
	public void createModelWithExistingName() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelWithName");
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals(Boolean.TRUE, mavContainer.getAttribute("name"));
	}

	@Test
	public void createModelWithNullAttribute() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelWithNullAttribute");
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertTrue(mavContainer.containsAttribute("name"));
		assertNull(mavContainer.getAttribute("name"));
	}
	
	@Test
	public void createModelExistingSessionAttributes() throws Exception {
		sessionAttributeStore.storeAttribute(webRequest, "sessAttr", "sessAttrValue");

		// Resolve successfully handler session attribute once
		assertTrue(handlerSessionAttributeStore.isHandlerSessionAttribute("sessAttr", null));
		
		ModelFactory modelFactory = createModelFactory("model", Model.class);
		modelFactory.initModel(webRequest, mavContainer, handleMethod);

		assertEquals("sessAttrValue", mavContainer.getAttribute("sessAttr"));
	}
	
	@Test
	public void updateBindingResult() throws Exception {
		String attrName = "attr1";
		Object attrValue = new Object();
		mavContainer.addAttribute(attrName, attrValue);
		
		WebDataBinder dataBinder = new WebDataBinder(attrValue, attrName);

		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, attrValue, attrName)).andReturn(dataBinder);
		replay(binderFactory);
		
		ModelFactory modelFactory = new ModelFactory(null, binderFactory, handlerSessionAttributeStore);
		modelFactory.updateModel(webRequest, mavContainer, new SimpleSessionStatus());

		assertEquals(attrValue, mavContainer.getModel().remove(attrName));
		assertSame(dataBinder.getBindingResult(), mavContainer.getModel().remove(bindingResultKey(attrName)));
		assertEquals(0, mavContainer.getModel().size());
		
		verify(binderFactory);
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
		
		return new ModelFactory(Arrays.asList(handlerMethod), null, handlerSessionAttributeStore);
	}
	
	@SessionAttributes("sessAttr")
	private static class ModelHandler {
		
		@SuppressWarnings("unused")
		@ModelAttribute
		public void model(Model model) {
			model.addAttribute("model", Boolean.TRUE);
		}

		@SuppressWarnings("unused")
		@ModelAttribute("name")
		public Boolean modelWithName() {
			return Boolean.TRUE;
		}

		@SuppressWarnings("unused")
		@ModelAttribute
		public Boolean modelWithDefaultName() {
			return Boolean.TRUE;
		}

		@SuppressWarnings("unused")
		@ModelAttribute("name")
		public Boolean modelWithNullAttribute() {
			return null;
		}
		
		@SuppressWarnings("unused")
		public void handle() {
		}
	}
}

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

package org.springframework.web.method.annotation.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture for {@link ModelMethodProcessor} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ModelMethodProcessorTests {

	private ModelMethodProcessor resolver;
	
	private MethodParameter modelParameter;

	private MethodParameter modelReturnType;

	private MethodParameter mapParameter;

	private MethodParameter mapReturnType;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		this.resolver = new ModelMethodProcessor();
		
		Method modelMethod = getClass().getDeclaredMethod("model", Model.class); 
		this.modelParameter = new MethodParameter(modelMethod, 0);
		this.modelReturnType = new MethodParameter(modelMethod, -1);
		
		Method mapMethod = getClass().getDeclaredMethod("map", Map.class); 
		this.mapParameter = new MethodParameter(mapMethod, 0);
		this.mapReturnType = new MethodParameter(mapMethod, 0);

		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void usesResponseArgument() {
		assertFalse(resolver.usesResponseArgument(null));
	}
	
	@Test
	public void supportsParameter() {
		assertTrue(resolver.supportsParameter(modelParameter));
		assertTrue(resolver.supportsParameter(mapParameter));
	}

	@Test
	public void supportsReturnType() {
		assertTrue(resolver.supportsReturnType(modelReturnType));
		assertTrue(resolver.supportsReturnType(mapReturnType));
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		ExtendedModelMap model = new ExtendedModelMap();

		Object result = resolver.resolveArgument(modelParameter, model, webRequest, null);
		assertSame(model, result);
		
		result = resolver.resolveArgument(mapParameter, model, webRequest, null);
		assertSame(model, result);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void handleReturnValue() throws Exception {
		ExtendedModelMap implicitModel = new ExtendedModelMap();
		implicitModel.put("attr1", "value1");

		ExtendedModelMap returnValue = new ExtendedModelMap();
		returnValue.put("attr2", "value2");
		
		ModelAndViewContainer<?> mavContainer = new ModelAndViewContainer(implicitModel);
		resolver.handleReturnValue(returnValue , modelReturnType, mavContainer, webRequest);
		ModelMap actualModel = mavContainer.getModel();
		assertEquals("value1", actualModel.get("attr1"));
		assertEquals("value2", actualModel.get("attr2"));
		
		mavContainer = new ModelAndViewContainer(implicitModel);
		resolver.handleReturnValue(returnValue , mapReturnType, mavContainer, webRequest);
		actualModel = mavContainer.getModel();
		assertEquals("value1", actualModel.get("attr1"));
		assertEquals("value2", actualModel.get("attr2"));
	}
	
	@SuppressWarnings("unused")
	private Model model(Model model) {
		return null;
	}

	@SuppressWarnings("unused")
	private Map<String, Object> map(Map<String, Object> map) {
		return null;
	}
}

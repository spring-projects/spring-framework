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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture with {@link ModelMethodProcessor}.
 * 
 * @author Rossen Stoyanchev
 */
public class ModelMethodProcessorTests {

	private ModelMethodProcessor processor;
	
	private ModelAndViewContainer mavContainer;
	
	private MethodParameter paramModel;

	private MethodParameter returnParamModel;

	private MethodParameter paramMap;

	private MethodParameter returnParamMap;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		processor = new ModelMethodProcessor();
		mavContainer = new ModelAndViewContainer();
		
		Method method = getClass().getDeclaredMethod("model", Model.class); 
		paramModel = new MethodParameter(method, 0);
		returnParamModel = new MethodParameter(method, -1);
		
		method = getClass().getDeclaredMethod("map", Map.class); 
		paramMap = new MethodParameter(method, 0);
		returnParamMap = new MethodParameter(method, 0);

		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportsParameter() {
		assertTrue(processor.supportsParameter(paramModel));
		assertTrue(processor.supportsParameter(paramMap));
	}

	@Test
	public void supportsReturnType() {
		assertTrue(processor.supportsReturnType(returnParamModel));
		assertTrue(processor.supportsReturnType(returnParamMap));
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		Object result = processor.resolveArgument(paramModel, mavContainer, webRequest, null);
		assertSame(mavContainer.getModel(), result);
		
		result = processor.resolveArgument(paramMap, mavContainer, webRequest, null);
		assertSame(mavContainer.getModel(), result);
	}
	
	@Test
	public void handleModelReturnValue() throws Exception {
		mavContainer.addAttribute("attr1", "value1");
		ModelMap returnValue = new ModelMap("attr2", "value2");

		processor.handleReturnValue(returnValue , returnParamModel, mavContainer, webRequest);
		
		assertEquals("value1", mavContainer.getModel().get("attr1"));
		assertEquals("value2", mavContainer.getModel().get("attr2"));
	}

	@Test
	public void handleMapReturnValue() throws Exception {
		mavContainer.addAttribute("attr1", "value1");
		Map<String, Object> returnValue = new ModelMap("attr2", "value2");
		
		processor.handleReturnValue(returnValue , returnParamMap, mavContainer, webRequest);

		assertEquals("value1", mavContainer.getModel().get("attr1"));
		assertEquals("value2", mavContainer.getModel().get("attr2"));
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
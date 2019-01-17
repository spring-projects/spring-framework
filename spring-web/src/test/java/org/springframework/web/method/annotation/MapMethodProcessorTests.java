/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.MapMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class MapMethodProcessorTests {

	private MapMethodProcessor processor;

	private ModelAndViewContainer mavContainer;

	private MethodParameter paramMap;

	private MethodParameter returnParamMap;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		processor = new MapMethodProcessor();
		mavContainer = new ModelAndViewContainer();

		Method method = getClass().getDeclaredMethod("map", Map.class);
		paramMap = new MethodParameter(method, 0);
		returnParamMap = new MethodParameter(method, 0);

		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportsParameter() {
		assertTrue(processor.supportsParameter(paramMap));
	}

	@Test
	public void supportsReturnType() {
		assertTrue(processor.supportsReturnType(returnParamMap));
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		assertSame(mavContainer.getModel(), processor.resolveArgument(paramMap, mavContainer, webRequest, null));
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
	private Map<String, Object> map(Map<String, Object> map) {
		return null;
	}

}

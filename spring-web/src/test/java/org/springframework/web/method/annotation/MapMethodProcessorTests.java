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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.Assert.*;

/**
 * Test fixture with
 * {@link org.springframework.web.method.annotation.MapMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class MapMethodProcessorTests {

	private MapMethodProcessor processor;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private final ResolvableMethod resolvable =
			ResolvableMethod.on(getClass()).annotPresent(RequestMapping.class).build();


	@Before
	public void setUp() throws Exception {
		this.processor = new MapMethodProcessor();
		this.mavContainer = new ModelAndViewContainer();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.processor.supportsParameter(
				this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class)));
		assertFalse(this.processor.supportsParameter(
				this.resolvable.annotPresent(RequestBody.class).arg(Map.class, String.class, Object.class)));
	}

	@Test
	public void supportsReturnType() {
		assertTrue(this.processor.supportsReturnType(this.resolvable.returnType()));
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		MethodParameter param = this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class);
		assertSame(this.mavContainer.getModel(),
				this.processor.resolveArgument(param, this.mavContainer, this.webRequest, null));
	}

	@Test
	public void handleMapReturnValue() throws Exception {
		this.mavContainer.addAttribute("attr1", "value1");
		Map<String, Object> returnValue = new ModelMap("attr2", "value2");

		this.processor.handleReturnValue(
				returnValue , this.resolvable.returnType(), this.mavContainer, this.webRequest);

		assertEquals("value1", mavContainer.getModel().get("attr1"));
		assertEquals("value2", mavContainer.getModel().get("attr2"));
	}


	@SuppressWarnings("unused")
	@RequestMapping
	private Map<String, Object> handle(
			Map<String, Object> map,
			@RequestBody Map<String, Object> annotMap) {

		return null;
	}

}

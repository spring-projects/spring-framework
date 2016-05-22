/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link MatrixVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class MatrixVariablesMapMethodArgumentResolverTests {

	private MatrixVariableMapMethodArgumentResolver resolver;

	private MethodParameter paramString;
	private MethodParameter paramMap;
	private MethodParameter paramMultivalueMap;
	private MethodParameter paramMapForPathVar;
	private MethodParameter paramMapWithName;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;


	@Before
	public void setUp() throws Exception {
		this.resolver = new MatrixVariableMapMethodArgumentResolver();

		Method method = getClass().getMethod("handle", String.class,
				Map.class, MultiValueMap.class, MultiValueMap.class, Map.class);

		this.paramString = new SynthesizingMethodParameter(method, 0);
		this.paramMap = new SynthesizingMethodParameter(method, 1);
		this.paramMultivalueMap = new SynthesizingMethodParameter(method, 2);
		this.paramMapForPathVar = new SynthesizingMethodParameter(method, 3);
		this.paramMapWithName = new SynthesizingMethodParameter(method, 4);

		this.mavContainer = new ModelAndViewContainer();
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Map<String, MultiValueMap<String, String>> params = new LinkedHashMap<String, MultiValueMap<String, String>>();
		this.request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, params);
	}

	@Test
	public void supportsParameter() {
		assertFalse(resolver.supportsParameter(paramString));
		assertTrue(resolver.supportsParameter(paramMap));
		assertTrue(resolver.supportsParameter(paramMultivalueMap));
		assertTrue(resolver.supportsParameter(paramMapForPathVar));
		assertFalse(resolver.supportsParameter(paramMapWithName));
	}

	@Test
	public void resolveArgument() throws Exception {
		MultiValueMap<String, String> params = getMatrixVariables("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");
		params.add("year", "2012");

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) this.resolver.resolveArgument(
				this.paramMap, this.mavContainer, this.webRequest, null);

		assertEquals("red", map.get("colors"));

		@SuppressWarnings("unchecked")
		MultiValueMap<String, String> multivalueMap = (MultiValueMap<String, String>) this.resolver.resolveArgument(
				this.paramMultivalueMap, this.mavContainer, this.webRequest, null);

		assertEquals(Arrays.asList("red", "green", "blue"), multivalueMap.get("colors"));
	}

	@Test
	public void resolveArgumentPathVariable() throws Exception {
		MultiValueMap<String, String> params1 = getMatrixVariables("cars");
		params1.add("colors", "red");
		params1.add("colors", "purple");

		MultiValueMap<String, String> params2 = getMatrixVariables("planes");
		params2.add("colors", "yellow");
		params2.add("colors", "orange");

		@SuppressWarnings("unchecked")
		Map<String, String> mapForPathVar = (Map<String, String>) this.resolver.resolveArgument(
				this.paramMapForPathVar, this.mavContainer, this.webRequest, null);

		assertEquals(Arrays.asList("red", "purple"), mapForPathVar.get("colors"));

		@SuppressWarnings("unchecked")
		Map<String, String> mapAll = (Map<String, String>) this.resolver.resolveArgument(
				this.paramMap, this.mavContainer, this.webRequest, null);

		assertEquals("red", mapAll.get("colors"));
	}

	@Test
	public void resolveArgumentNoParams() throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) this.resolver.resolveArgument(
				this.paramMap, this.mavContainer, this.webRequest, null);

		assertEquals(Collections.emptyMap(), map);
	}

	@Test
	public void resolveArgumentNoMatch() throws Exception {
		MultiValueMap<String, String> params2 = getMatrixVariables("planes");
		params2.add("colors", "yellow");
		params2.add("colors", "orange");

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) this.resolver.resolveArgument(
				this.paramMapForPathVar, this.mavContainer, this.webRequest, null);

		assertEquals(Collections.emptyMap(), map);
	}


	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getMatrixVariables(String pathVarName) {
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) this.request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		matrixVariables.put(pathVarName, params);

		return params;
	}


	public void handle(
			String stringArg,
			@MatrixVariable Map<String, String> map,
			@MatrixVariable MultiValueMap<String, String> multivalueMap,
			@MatrixVariable(pathVar="cars") MultiValueMap<String, String> mapForPathVar,
			@MatrixVariable("name") Map<String, String> mapWithName) {
	}

}

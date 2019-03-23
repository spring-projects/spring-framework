/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.method.MvcAnnotationPredicates.matrixAttribute;

/**
 * Test fixture with {@link MatrixVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class MatrixVariablesMapMethodArgumentResolverTests {

	private MatrixVariableMapMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	private final ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {
		this.resolver = new MatrixVariableMapMethodArgumentResolver();
		this.mavContainer = new ModelAndViewContainer();
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Map<String, MultiValueMap<String, String>> params = new LinkedHashMap<>();
		this.request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, params);
	}


	@Test
	public void supportsParameter() {

		assertFalse(this.resolver.supportsParameter(this.testMethod.arg(String.class)));

		assertTrue(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().noName())
				.arg(Map.class, String.class, String.class)));

		assertTrue(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().noPathVar())
				.arg(MultiValueMap.class, String.class, String.class)));

		assertTrue(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().pathVar("cars"))
				.arg(MultiValueMap.class, String.class, String.class)));

		assertFalse(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().name("name"))
				.arg(Map.class, String.class, String.class)));
	}

	@Test
	public void resolveArgument() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");
		params.add("year", "2012");

		MethodParameter param = this.testMethod.annot(matrixAttribute().noName())
				.arg(Map.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>)
				this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);

		assertEquals("red", map.get("colors"));

		param = this.testMethod
				.annot(matrixAttribute().noPathVar())
				.arg(MultiValueMap.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		MultiValueMap<String, String> multivalueMap = (MultiValueMap<String, String>)
				this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);

		assertEquals(Arrays.asList("red", "green", "blue"), multivalueMap.get("colors"));
	}

	@Test
	public void resolveArgumentPathVariable() throws Exception {
		MultiValueMap<String, String> params1 = getVariablesFor("cars");
		params1.add("colors", "red");
		params1.add("colors", "purple");

		MultiValueMap<String, String> params2 = getVariablesFor("planes");
		params2.add("colors", "yellow");
		params2.add("colors", "orange");

		MethodParameter param = this.testMethod.annot(matrixAttribute().pathVar("cars"))
				.arg(MultiValueMap.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> mapForPathVar = (Map<String, String>) this.resolver.resolveArgument(
				param, this.mavContainer, this.webRequest, null);

		assertEquals(Arrays.asList("red", "purple"), mapForPathVar.get("colors"));

		param = this.testMethod.annot(matrixAttribute().noName()).arg(Map.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> mapAll = (Map<String, String>)
				this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);

		assertEquals("red", mapAll.get("colors"));
	}

	@Test
	public void resolveArgumentNoParams() throws Exception {

		MethodParameter param = this.testMethod.annot(matrixAttribute().noName())
				.arg(Map.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>)
				this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);

		assertEquals(Collections.emptyMap(), map);
	}

	@Test
	public void resolveArgumentNoMatch() throws Exception {
		MultiValueMap<String, String> params2 = getVariablesFor("planes");
		params2.add("colors", "yellow");
		params2.add("colors", "orange");

		MethodParameter param = this.testMethod.annot(matrixAttribute().pathVar("cars"))
				.arg(MultiValueMap.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>)
				this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);

		assertEquals(Collections.emptyMap(), map);
	}


	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getVariablesFor(String pathVarName) {
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) this.request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		matrixVariables.put(pathVarName, params);
		return params;
	}


	@SuppressWarnings("unused")
	public void handle(
			String stringArg,
			@MatrixVariable Map<String, String> map,
			@MatrixVariable MultiValueMap<String, String> multivalueMap,
			@MatrixVariable(pathVar="cars") MultiValueMap<String, String> mapForPathVar,
			@MatrixVariable("name") Map<String, String> mapWithName) {
	}

}

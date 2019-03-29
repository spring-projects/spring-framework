/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.ServletRequestBindingException;
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
 * @author Rossen Stoyanchev
 */
public class MatrixVariablesMethodArgumentResolverTests {

	private MatrixVariableMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	private ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {
		this.resolver = new MatrixVariableMethodArgumentResolver();
		this.mavContainer = new ModelAndViewContainer();
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Map<String, MultiValueMap<String, String>> params = new LinkedHashMap<>();
		this.request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, params);
	}


	@Test
	public void supportsParameter() {

		assertFalse(this.resolver.supportsParameter(this.testMethod.arg(String.class)));

		assertTrue(this.resolver.supportsParameter(
				this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class)));

		assertTrue(this.resolver.supportsParameter(
				this.testMethod.annot(matrixAttribute().name("year")).arg(int.class)));
	}

	@Test
	public void resolveArgument() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);

		assertEquals(Arrays.asList("red", "green", "blue"),
				this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null));
	}

	@Test
	public void resolveArgumentPathVariable() throws Exception {
		getVariablesFor("cars").add("year", "2006");
		getVariablesFor("bikes").add("year", "2005");
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);

		assertEquals("2006", this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null));
	}

	@Test
	public void resolveArgumentDefaultValue() throws Exception {
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);
		assertEquals("2013", resolver.resolveArgument(param, this.mavContainer, this.webRequest, null));
	}

	@Test(expected = ServletRequestBindingException.class)
	public void resolveArgumentMultipleMatches() throws Exception {
		getVariablesFor("var1").add("colors", "red");
		getVariablesFor("var2").add("colors", "green");
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);

		this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);
	}

	@Test(expected = ServletRequestBindingException.class)
	public void resolveArgumentRequired() throws Exception {
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);
		this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null);
	}

	@Test
	public void resolveArgumentNoMatch() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("anotherYear", "2012");
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);

		assertEquals("2013", this.resolver.resolveArgument(param, this.mavContainer, this.webRequest, null));
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


	public void handle(
			String stringArg,
			@MatrixVariable List<String> colors,
			@MatrixVariable(name = "year", pathVar = "cars", required = false, defaultValue = "2013") int preferredYear) {
	}

}

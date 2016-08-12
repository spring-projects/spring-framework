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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.ServletRequestBindingException;
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
public class MatrixVariablesMethodArgumentResolverTests {

	private MatrixVariableMethodArgumentResolver resolver;

	private MethodParameter paramString;
	private MethodParameter paramColors;
	private MethodParameter paramYear;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;


	@Before
	public void setUp() throws Exception {
		this.resolver = new MatrixVariableMethodArgumentResolver();

		Method method = getClass().getMethod("handle", String.class, List.class, int.class);
		this.paramString = new MethodParameter(method, 0);
		this.paramColors = new MethodParameter(method, 1);
		this.paramYear = new MethodParameter(method, 2);

		this.paramColors.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());

		this.mavContainer = new ModelAndViewContainer();
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Map<String, MultiValueMap<String, String>> params = new LinkedHashMap<>();
		this.request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, params);
	}

	@Test
	public void supportsParameter() {
		assertFalse(resolver.supportsParameter(paramString));
		assertTrue(resolver.supportsParameter(paramColors));
		assertTrue(resolver.supportsParameter(paramYear));
	}

	@Test
	public void resolveArgument() throws Exception {

		MultiValueMap<String, String> params = getMatrixVariables("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");

		assertEquals(Arrays.asList("red", "green", "blue"),
				this.resolver.resolveArgument(this.paramColors, this.mavContainer, this.webRequest, null));
	}

	@Test
	public void resolveArgumentPathVariable() throws Exception {

		getMatrixVariables("cars").add("year", "2006");
		getMatrixVariables("bikes").add("year", "2005");

		assertEquals("2006", this.resolver.resolveArgument(this.paramYear, this.mavContainer, this.webRequest, null));
	}

	@Test
	public void resolveArgumentDefaultValue() throws Exception {
		assertEquals("2013", resolver.resolveArgument(this.paramYear, this.mavContainer, this.webRequest, null));
	}

	@Test(expected = ServletRequestBindingException.class)
	public void resolveArgumentMultipleMatches() throws Exception {

		getMatrixVariables("var1").add("colors", "red");
		getMatrixVariables("var2").add("colors", "green");

		this.resolver.resolveArgument(this.paramColors, this.mavContainer, this.webRequest, null);
	}

	@Test(expected = ServletRequestBindingException.class)
	public void resolveArgumentRequired() throws Exception {
		resolver.resolveArgument(this.paramColors, this.mavContainer, this.webRequest, null);
	}

	@Test
	public void resolveArgumentNoMatch() throws Exception {

		MultiValueMap<String, String> params = getMatrixVariables("cars");
		params.add("anotherYear", "2012");

		assertEquals("2013", this.resolver.resolveArgument(this.paramYear, this.mavContainer, this.webRequest, null));
	}


	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getMatrixVariables(String pathVarName) {

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
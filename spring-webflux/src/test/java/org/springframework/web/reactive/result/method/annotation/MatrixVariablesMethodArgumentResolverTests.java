/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.method.MvcAnnotationPredicates.matrixAttribute;

/**
 * Unit tests for {@link MatrixVariableMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class MatrixVariablesMethodArgumentResolverTests {

	private MatrixVariableMethodArgumentResolver resolver =
			new MatrixVariableMethodArgumentResolver(null, ReactiveAdapterRegistry.getSharedInstance());

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();


	@Before
	public void setUp() throws Exception {
		this.exchange.getAttributes().put(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, new LinkedHashMap<>());
	}


	@Test
	public void supportsParameter() {

		assertFalse(this.resolver.supportsParameter(this.testMethod.arg(String.class)));

		assertTrue(this.resolver.supportsParameter(this.testMethod
				.annot(matrixAttribute().noName()).arg(List.class, String.class)));

		assertTrue(this.resolver.supportsParameter(this.testMethod
				.annot(matrixAttribute().name("year")).arg(int.class)));
	}

	@Test
	public void resolveArgument() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);

		assertEquals(Arrays.asList("red", "green", "blue"),
				this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO));
	}

	@Test
	public void resolveArgumentPathVariable() throws Exception {
		getVariablesFor("cars").add("year", "2006");
		getVariablesFor("bikes").add("year", "2005");
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);

		Object actual = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
		assertEquals(2006, actual);
	}

	@Test
	public void resolveArgumentDefaultValue() throws Exception {
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);
		Object actual = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
		assertEquals(2013, actual);
	}

	@Test(expected = ServerErrorException.class)
	public void resolveArgumentMultipleMatches() throws Exception {
		getVariablesFor("var1").add("colors", "red");
		getVariablesFor("var2").add("colors", "green");

		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);
		this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
	}

	@Test(expected = ServerWebInputException.class)
	public void resolveArgumentRequired() throws Exception {
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);
		this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
	}

	@Test
	public void resolveArgumentNoMatch() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("anotherYear", "2012");
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);

		Object actual = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
		assertEquals(2013, actual);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getVariablesFor(String pathVarName) {
		Map<String, MultiValueMap<String, String>> matrixVariables =
				this.exchange.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		matrixVariables.put(pathVarName, params);
		return params;
	}


	@SuppressWarnings("unused")
	void handle(
			String stringArg,
			@MatrixVariable List<String> colors,
			@MatrixVariable(name = "year", pathVar = "cars", required = false, defaultValue = "2013") int preferredYear) {
	}

}

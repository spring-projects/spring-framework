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
package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.matrixAttribute;

/**
 * Unit tests for {@link MatrixVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class MatrixVariablesMethodArgumentResolverTests {

	private MatrixVariableMethodArgumentResolver resolver =
			new MatrixVariableMethodArgumentResolver(null, ReactiveAdapterRegistry.getSharedInstance());

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();


	@BeforeEach
	public void setUp() throws Exception {
		this.exchange.getAttributes().put(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, new LinkedHashMap<>());
	}


	@Test
	public void supportsParameter() {

		assertThat(this.resolver.supportsParameter(this.testMethod.arg(String.class))).isFalse();

		assertThat(this.resolver.supportsParameter(this.testMethod
				.annot(matrixAttribute().noName()).arg(List.class, String.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.testMethod
				.annot(matrixAttribute().name("year")).arg(int.class))).isTrue();
	}

	@Test
	public void resolveArgument() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);

		assertThat(this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO)).isEqualTo(Arrays.asList("red", "green", "blue"));
	}

	@Test
	public void resolveArgumentPathVariable() throws Exception {
		getVariablesFor("cars").add("year", "2006");
		getVariablesFor("bikes").add("year", "2005");
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);

		Object actual = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
		assertThat(actual).isEqualTo(2006);
	}

	@Test
	public void resolveArgumentDefaultValue() throws Exception {
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);
		Object actual = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
		assertThat(actual).isEqualTo(2013);
	}

	@Test
	public void resolveArgumentMultipleMatches() throws Exception {
		getVariablesFor("var1").add("colors", "red");
		getVariablesFor("var2").add("colors", "green");

		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);
		assertThatExceptionOfType(ServerErrorException.class).isThrownBy(() ->
				this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO));
	}

	@Test
	public void resolveArgumentRequired() throws Exception {
		MethodParameter param = this.testMethod.annot(matrixAttribute().noName()).arg(List.class, String.class);
		assertThatExceptionOfType(ServerWebInputException.class).isThrownBy(() ->
				this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO));
	}

	@Test
	public void resolveArgumentNoMatch() throws Exception {
		MultiValueMap<String, String> params = getVariablesFor("cars");
		params.add("anotherYear", "2012");
		MethodParameter param = this.testMethod.annot(matrixAttribute().name("year")).arg(int.class);

		Object actual = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);
		assertThat(actual).isEqualTo(2013);
	}

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

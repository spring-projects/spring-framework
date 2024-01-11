/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.matrixAttribute;

/**
 * Tests for {@link MatrixVariableMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class MatrixVariablesMapMethodArgumentResolverTests {

	private final MatrixVariableMapMethodArgumentResolver resolver =
			new MatrixVariableMapMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private final ResolvableMethod testMethod = ResolvableMethod.on(this.getClass()).named("handle").build();


	@BeforeEach
	void setUp() {
		this.exchange.getAttributes().put(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, new LinkedHashMap<>());
	}


	@Test
	void supportsParameter() {

		assertThat(this.resolver.supportsParameter(this.testMethod.arg(String.class))).isFalse();

		assertThat(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().noName())
				.arg(Map.class, String.class, String.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().noPathVar())
				.arg(MultiValueMap.class, String.class, String.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().pathVar("cars"))
				.arg(MultiValueMap.class, String.class, String.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.testMethod.annot(matrixAttribute().name("name"))
				.arg(Map.class, String.class, String.class))).isFalse();
	}

	@Test
	void resolveArgument() throws Exception {
		MultiValueMap<String, String> params = getMatrixVariables("cars");
		params.add("colors", "red");
		params.add("colors", "green");
		params.add("colors", "blue");
		params.add("year", "2012");

		MethodParameter param = this.testMethod.annot(matrixAttribute().noName())
				.arg(Map.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> map =
				(Map<String, String>) this.resolver.resolveArgument(
						param, new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(map).isNotNull();
		assertThat(map.get("colors")).isEqualTo("red");

		param = this.testMethod
				.annot(matrixAttribute().noPathVar())
				.arg(MultiValueMap.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		MultiValueMap<String, String> multivalueMap =
				(MultiValueMap<String, String>) this.resolver.resolveArgument(
						param, new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(multivalueMap.get("colors")).isEqualTo(Arrays.asList("red", "green", "blue"));
	}

	@Test
	void resolveArgumentPathVariable() {
		MultiValueMap<String, String> params1 = getMatrixVariables("cars");
		params1.add("colors", "red");
		params1.add("colors", "purple");

		MultiValueMap<String, String> params2 = getMatrixVariables("planes");
		params2.add("colors", "yellow");
		params2.add("colors", "orange");

		MethodParameter param = this.testMethod.annot(matrixAttribute().pathVar("cars"))
				.arg(MultiValueMap.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, ?> mapForPathVar = (Map<String, ?>)
				this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(mapForPathVar).isNotNull();
		assertThat(mapForPathVar.get("colors")).isEqualTo(Arrays.asList("red", "purple"));

		param = this.testMethod.annot(matrixAttribute().noName()).arg(Map.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> mapAll = (Map<String, String>)
				this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(mapAll).isNotNull();
		assertThat(mapAll.get("colors")).isEqualTo("red");
	}

	@Test
	void resolveMultiValueMapArgumentNoParams() {

		MethodParameter param = this.testMethod.annot(matrixAttribute().noPathVar())
				.arg(MultiValueMap.class, String.class, String.class);

		Object result = this.resolver.resolveArgument(param,
				new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(result).isInstanceOf(MultiValueMap.class)
				.asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
	}

	@Test
	void resolveArgumentNoParams() {

		MethodParameter param = this.testMethod.annot(matrixAttribute().noName())
				.arg(Map.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>)
				this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(map).isEqualTo(Collections.emptyMap());
	}

	@Test
	void resolveArgumentNoMatch() {
		MultiValueMap<String, String> params2 = getMatrixVariables("planes");
		params2.add("colors", "yellow");
		params2.add("colors", "orange");

		MethodParameter param = this.testMethod.annot(matrixAttribute().pathVar("cars"))
				.arg(MultiValueMap.class, String.class, String.class);

		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) this.resolver.resolveArgument(
				param, new BindingContext(), this.exchange).block(Duration.ZERO);

		assertThat(map).isEqualTo(Collections.emptyMap());
	}


	private MultiValueMap<String, String> getMatrixVariables(String pathVarName) {
		Map<String, MultiValueMap<String, String>> matrixVariables =
				this.exchange.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		matrixVariables.put(pathVarName, params);

		return params;
	}


	@SuppressWarnings("unused")
	void handle(
			String stringArg,
			@MatrixVariable Map<String, String> map,
			@MatrixVariable MultiValueMap<String, String> multivalueMap,
			@MatrixVariable(pathVar="cars") MultiValueMap<String, String> mapForPathVar,
			@MatrixVariable("name") Map<String, String> mapWithName) {
	}

}

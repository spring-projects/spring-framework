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
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Unit tests for {@link ModelMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelMethodArgumentResolverTests {

	private final ModelMethodArgumentResolver resolver =
			new ModelMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

	private final ResolvableMethod resolvable = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(Model.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(ModelMap.class))).isTrue();
		assertThat(this.resolver.supportsParameter(
				this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.resolvable.arg(Object.class))).isFalse();
		assertThat(this.resolver.supportsParameter(
				this.resolvable.annotPresent(RequestBody.class).arg(Map.class, String.class, Object.class))).isFalse();
	}

	@Test
	public void resolveArgument() {
		testResolveArgument(this.resolvable.arg(Model.class));
		testResolveArgument(this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class));
		testResolveArgument(this.resolvable.arg(ModelMap.class));
	}

	private void testResolveArgument(MethodParameter parameter) {
		BindingContext context = new BindingContext();
		Object result = this.resolver.resolveArgument(parameter, context, this.exchange).block(Duration.ZERO);
		assertThat(result).isSameAs(context.getModel());
	}


	@SuppressWarnings("unused")
	void handle(
			Model model,
			Map<String, Object> map,
			@RequestBody Map<String, Object> annotatedMap,
			ModelMap modelMap,
			Object object) {}

}

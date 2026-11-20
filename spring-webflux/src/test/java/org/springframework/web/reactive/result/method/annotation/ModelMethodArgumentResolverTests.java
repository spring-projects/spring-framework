/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Tests for {@link ModelMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class ModelMethodArgumentResolverTests {

	private final ModelMethodArgumentResolver resolver =
			new ModelMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final ServerWebExchange exchange = MockServerWebExchange.from(get("/"));

	private final ResolvableMethod resolvable = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(Model.class))).isTrue();
		assertThat(this.resolver.supportsParameter(
				this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.resolvable.arg(Object.class))).isFalse();
		assertThat(this.resolver.supportsParameter(this.resolvable.arg(ModelMap.class))).isFalse();
		assertThat(this.resolver.supportsParameter(
				this.resolvable.annotPresent(RequestBody.class).arg(Map.class, String.class, Object.class))).isFalse();
	}

	@Test
	void resolveArgument() {
		testResolveArgument(this.resolvable.arg(Model.class));
		testResolveArgument(this.resolvable.annotNotPresent().arg(Map.class, String.class, Object.class));

		assertThatIllegalStateException()
				.isThrownBy(() -> testResolveArgument(this.resolvable.arg(Object.class)))
				.withMessage("Unexpected method parameter type: " + Object.class.getName());
		assertThatIllegalStateException()
				.isThrownBy(() -> testResolveArgument(this.resolvable.arg(ModelMap.class)))
				.withMessage("Unexpected method parameter type: " + ModelMap.class.getName());
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

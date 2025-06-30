/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ExpressionValueMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class ExpressionValueMethodArgumentResolverTests {

	private ExpressionValueMethodArgumentResolver resolver;

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private MethodParameter paramSystemProperty;
	private MethodParameter paramNotSupported;
	private MethodParameter paramAlsoNotSupported;


	@BeforeEach
	void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new ExpressionValueMethodArgumentResolver(context.getBeanFactory(), adapterRegistry);

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		this.paramSystemProperty = new MethodParameter(method, 0);
		this.paramNotSupported = new MethodParameter(method, 1);
		this.paramAlsoNotSupported = new MethodParameter(method, 2);
	}


	@Test
	void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.paramSystemProperty)).isTrue();
	}

	@Test
	void doesNotSupport() {
		assertThat(this.resolver.supportsParameter(this.paramNotSupported)).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.paramAlsoNotSupported))
			.withMessageStartingWith("ExpressionValueMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	void resolveSystemProperty() {
		System.setProperty("systemProperty", "22");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(
					this.paramSystemProperty, new BindingContext(), this.exchange);

			Object value = mono.block();
			assertThat(value).isEqualTo(22);
		}
		finally {
			System.clearProperty("systemProperty");
		}

	}

	// TODO: test with expression for ServerWebExchange


	@SuppressWarnings("unused")
	public void params(
			@Value("#{systemProperties.systemProperty}") int param1,
			String notSupported,
			@Value("#{systemProperties.foo}") Mono<String> alsoNotSupported) {
	}

}

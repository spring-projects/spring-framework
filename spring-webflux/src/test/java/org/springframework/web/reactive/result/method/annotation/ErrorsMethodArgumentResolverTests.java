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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ErrorsMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class ErrorsMethodArgumentResolverTests {

	private final ErrorsMethodArgumentResolver resolver =
			new ErrorsMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final BindingContext bindingContext = new BindingContext();

	private final MockServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/path"));

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	void supports() {
		MethodParameter parameter = this.testMethod.arg(Errors.class);
		assertThat(this.resolver.supportsParameter(parameter)).isTrue();

		parameter = this.testMethod.arg(BindingResult.class);
		assertThat(this.resolver.supportsParameter(parameter)).isTrue();

		parameter = this.testMethod.arg(ResolvableType.forClassWithGenerics(Mono.class, Errors.class));
		assertThat(this.resolver.supportsParameter(parameter)).isTrue();

		parameter = this.testMethod.arg(String.class);
		assertThat(this.resolver.supportsParameter(parameter)).isFalse();
	}

	@Test
	void resolveWithInferredModelAttributeName() {
		BindingResult bindingResult = createBindingResult(new Foo(), "foo");
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "foo", bindingResult);

		MethodParameter parameter = this.testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isSameAs(bindingResult);
	}

	@Test
	void resolveWithCustomModelAttributeNameConfiguredViaValueAttribute() {
		BindingResult bindingResult = createBindingResult(new Foo(), "custom");
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "custom", bindingResult);

		ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handleWithCustomModelAttributeNameViaValueAttribute").build();

		MethodParameter parameter = testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isSameAs(bindingResult);
	}

	@Test
	void resolveWithCustomModelAttributeNameConfiguredViaNameAttribute() {
		BindingResult bindingResult = createBindingResult(new Foo(), "custom");
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "custom", bindingResult);

		ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handleWithCustomModelAttributeNameViaNameAttribute").build();

		MethodParameter parameter = testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isSameAs(bindingResult);
	}

	@Test
	void resolveWithMono() {
		BindingResult bindingResult = createBindingResult(new Foo(), "foo");
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "foo", Mono.just(bindingResult));

		MethodParameter parameter = this.testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isSameAs(bindingResult);
	}

	@Test
	void resolveWithMonoAndCustomModelAttributeNameConfiguredViaValueAttribute() {
		BindingResult bindingResult = createBindingResult(new Foo(), "custom");
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "custom", Mono.just(bindingResult));

		ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handleWithCustomModelAttributeNameViaValueAttribute").build();

		MethodParameter parameter = testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertThat(actual).isSameAs(bindingResult);
	}

	@Test
	void resolveWithMonoOnBindingResultAndModelAttribute() {
		MethodParameter parameter = this.testMethod.arg(BindingResult.class);
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
						.block(Duration.ofMillis(5000)))
			.withMessageContaining("An @ModelAttribute and an Errors/BindingResult argument " +
					"cannot both be declared with an async type wrapper.");
	}

	@Test  // SPR-16187
	void resolveWithBindingResultNotFound() {
		MethodParameter parameter = this.testMethod.arg(Errors.class);
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
						.block(Duration.ofMillis(5000)))
			.withMessageContaining("An Errors/BindingResult argument is expected " +
					"immediately after the @ModelAttribute argument");
	}

	private BindingResult createBindingResult(Foo target, String name) {
		DataBinder binder = this.bindingContext.createDataBinder(this.exchange, target, name);
		return binder.getBindingResult();
	}


	@SuppressWarnings("unused")
	private static class Foo {

		private String name;

		public Foo() {
		}

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@SuppressWarnings("unused")
	void handle(
			@ModelAttribute Foo foo,
			Errors errors,
			@ModelAttribute Mono<Foo> fooMono,
			BindingResult bindingResult,
			Mono<Errors> errorsMono,
			String string) {
	}

	@SuppressWarnings("unused")
	void handleWithCustomModelAttributeNameViaValueAttribute(
			@ModelAttribute("custom") Foo foo,
			Errors errors) {
	}

	@SuppressWarnings("unused")
	void handleWithCustomModelAttributeNameViaNameAttribute(
			@ModelAttribute(name = "custom") Foo foo,
			Errors errors) {
	}

}

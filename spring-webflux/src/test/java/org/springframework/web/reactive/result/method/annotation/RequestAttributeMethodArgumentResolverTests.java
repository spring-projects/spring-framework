/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Optional;

import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestAttribute;

/**
 * Unit tests for {@link RequestAttributeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeMethodArgumentResolverTests {

	private RequestAttributeMethodArgumentResolver resolver;

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass())
			.named("handleWithRequestAttribute").build();


	@BeforeEach
	@SuppressWarnings("resource")
	public void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestAttributeMethodArgumentResolver(context.getBeanFactory(), registry);
	}


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(
				this.testMethod.annot(requestAttribute().noName()).arg(Foo.class))).isTrue();

		// SPR-16158
		assertThat(this.resolver.supportsParameter(
				this.testMethod.annotPresent(RequestAttribute.class).arg(Mono.class, Foo.class))).isTrue();

		assertThat(this.resolver.supportsParameter(
				this.testMethod.annotNotPresent(RequestAttribute.class).arg())).isFalse();
	}

	@Test
	public void resolve() {
		MethodParameter param = this.testMethod.annot(requestAttribute().noName()).arg(Foo.class);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isSameAs(foo);
	}

	@Test
	public void resolveWithName() {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("specialFoo")).arg();
		Foo foo = new Foo();
		this.exchange.getAttributes().put("specialFoo", foo);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isSameAs(foo);
	}

	@Test
	public void resolveNotRequired() {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("foo").notRequired()).arg();
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isNull();

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isSameAs(foo);
	}

	@Test
	public void resolveOptional() {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("foo")).arg(Optional.class, Foo.class);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);

		assertThat(mono.block()).isNotNull();
		assertThat(mono.block().getClass()).isEqualTo(Optional.class);
		assertThat(((Optional<?>) mono.block()).isPresent()).isFalse();

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, bindingContext, this.exchange);

		assertThat(mono.block()).isNotNull();
		assertThat(mono.block().getClass()).isEqualTo(Optional.class);
		Optional<?> optional = (Optional<?>) mono.block();
		assertThat(optional.isPresent()).isTrue();
		assertThat(optional.get()).isSameAs(foo);
	}

	@Test  // SPR-16158
	public void resolveMonoParameter() {
		MethodParameter param = this.testMethod.annot(requestAttribute().noName()).arg(Mono.class, Foo.class);

		// Mono attribute
		Foo foo = new Foo();
		Mono<Foo> fooMono = Mono.just(foo);
		this.exchange.getAttributes().put("fooMono", fooMono);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block(Duration.ZERO)).isSameAs(fooMono);

		// RxJava Single attribute
		Single<Foo> singleMono = Single.just(foo);
		this.exchange.getAttributes().clear();
		this.exchange.getAttributes().put("fooMono", singleMono);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		Object value = mono.block(Duration.ZERO);
		boolean condition = value instanceof Mono;
		assertThat(condition).isTrue();
		assertThat(((Mono<?>) value).block(Duration.ZERO)).isSameAs(foo);

		// No attribute --> Mono.empty
		this.exchange.getAttributes().clear();
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block(Duration.ZERO)).isSameAs(Mono.empty());
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private void handleWithRequestAttribute(
			@RequestAttribute Foo foo,
			@RequestAttribute("specialFoo") Foo namedFoo,
			@RequestAttribute(name="foo", required = false) Foo notRequiredFoo,
			@RequestAttribute(name="foo") Optional<Foo> optionalFoo,
			@RequestAttribute Mono<Foo> fooMono,
			String notSupported) {
	}


	private static class Foo {
	}

}

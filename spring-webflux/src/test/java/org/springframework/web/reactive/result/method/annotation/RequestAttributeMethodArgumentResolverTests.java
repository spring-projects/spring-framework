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
import java.util.Optional;

import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebInputException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.method.MvcAnnotationPredicates.requestAttribute;

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


	@Before
	public void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestAttributeMethodArgumentResolver(context.getBeanFactory(), registry);
	}


	@Test
	public void supportsParameter() throws Exception {

		assertTrue(this.resolver.supportsParameter(
				this.testMethod.annot(requestAttribute().noName()).arg(Foo.class)));

		// SPR-16158
		assertTrue(this.resolver.supportsParameter(
				this.testMethod.annotPresent(RequestAttribute.class).arg(Mono.class, Foo.class)));

		assertFalse(this.resolver.supportsParameter(
				this.testMethod.annotNotPresent(RequestAttribute.class).arg()));
	}

	@Test
	public void resolve() throws Exception {
		MethodParameter param = this.testMethod.annot(requestAttribute().noName()).arg(Foo.class);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveWithName() throws Exception {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("specialFoo")).arg();
		Foo foo = new Foo();
		this.exchange.getAttributes().put("specialFoo", foo);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveNotRequired() throws Exception {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("foo").notRequired()).arg();
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertNull(mono.block());

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveOptional() throws Exception {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("foo")).arg(Optional.class, Foo.class);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);

		assertNotNull(mono.block());
		assertEquals(Optional.class, mono.block().getClass());
		assertFalse(((Optional<?>) mono.block()).isPresent());

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, bindingContext, this.exchange);

		assertNotNull(mono.block());
		assertEquals(Optional.class, mono.block().getClass());
		Optional<?> optional = (Optional<?>) mono.block();
		assertTrue(optional.isPresent());
		assertSame(foo, optional.get());
	}

	@Test // SPR-16158
	public void resolveMonoParameter() throws Exception {
		MethodParameter param = this.testMethod.annot(requestAttribute().noName()).arg(Mono.class, Foo.class);

		// Mono attribute
		Foo foo = new Foo();
		Mono<Foo> fooMono = Mono.just(foo);
		this.exchange.getAttributes().put("fooMono", fooMono);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(fooMono, mono.block(Duration.ZERO));

		// RxJava Single attribute
		Single<Foo> singleMono = Single.just(foo);
		this.exchange.getAttributes().clear();
		this.exchange.getAttributes().put("fooMono", singleMono);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		Object value = mono.block(Duration.ZERO);
		assertTrue(value instanceof Mono);
		assertSame(foo, ((Mono<?>) value).block(Duration.ZERO));

		// No attribute --> Mono.empty
		this.exchange.getAttributes().clear();
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(Mono.empty(), mono.block(Duration.ZERO));
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

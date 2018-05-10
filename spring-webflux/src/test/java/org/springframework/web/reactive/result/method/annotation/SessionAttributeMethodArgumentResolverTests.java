/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SessionAttributeMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class SessionAttributeMethodArgumentResolverTests {

	private SessionAttributeMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private WebSession session;

	private Method handleMethod;


	@Before
	@SuppressWarnings("resource")
	public void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new SessionAttributeMethodArgumentResolver(context.getBeanFactory(), adapterRegistry);
		this.session = mock(WebSession.class);
		this.exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("/")).session(this.session).build();
		this.handleMethod = ReflectionUtils.findMethod(getClass(), "handleWithSessionAttribute", (Class<?>[]) null);
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 0)));
		assertFalse(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 4)));
	}

	@Test
	public void resolve() {
		MethodParameter param = initMethodParameter(0);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		StepVerifier.create(mono).expectError(ServerWebInputException.class).verify();

		Foo foo = new Foo();
		when(this.session.getAttribute("foo")).thenReturn(foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveWithName() {
		MethodParameter param = initMethodParameter(1);
		Foo foo = new Foo();
		when(this.session.getAttribute("specialFoo")).thenReturn(foo);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveNotRequired() {
		MethodParameter param = initMethodParameter(2);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertNull(mono.block());

		Foo foo = new Foo();
		when(this.session.getAttribute("foo")).thenReturn(foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resolveOptional() {
		MethodParameter param = initMethodParameter(3);
		Optional<Object> actual = (Optional<Object>) this.resolver
				.resolveArgument(param, new BindingContext(), this.exchange).block();

		assertNotNull(actual);
		assertFalse(actual.isPresent());

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Foo foo = new Foo();
		when(this.session.getAttribute("foo")).thenReturn(foo);
		actual = (Optional<Object>) this.resolver.resolveArgument(param, bindingContext, this.exchange).block();

		assertNotNull(actual);
		assertTrue(actual.isPresent());
		assertSame(foo, actual.get());
	}


	private MethodParameter initMethodParameter(int parameterIndex) {
		MethodParameter param = new SynthesizingMethodParameter(this.handleMethod, parameterIndex);
		param.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
		GenericTypeResolver.resolveParameterType(param, this.resolver.getClass());
		return param;
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private void handleWithSessionAttribute(
			@SessionAttribute Foo foo,
			@SessionAttribute("specialFoo") Foo namedFoo,
			@SessionAttribute(name="foo", required = false) Foo notRequiredFoo,
			@SessionAttribute(name="foo") Optional<Foo> optionalFoo,
			String notSupported) {
	}

	private static class Foo {
	}

}

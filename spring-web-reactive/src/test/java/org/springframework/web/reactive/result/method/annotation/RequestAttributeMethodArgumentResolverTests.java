/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for {@link RequestAttributeMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class RequestAttributeMethodArgumentResolverTests {

	private RequestAttributeMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private Method handleMethod;


	@Before
	@SuppressWarnings("ConfusingArgumentToVarargsMethod")
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		this.resolver = new RequestAttributeMethodArgumentResolver(context.getBeanFactory());

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/");
		WebSessionManager sessionManager = new MockWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		this.handleMethod = ReflectionUtils.findMethod(getClass(), "handleWithRequestAttribute", (Class<?>[]) null);
	}


	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 0)));
		assertFalse(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 4)));
	}

	@Test
	public void resolve() throws Exception {
		MethodParameter param = initMethodParameter(0);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		TestSubscriber
				.subscribe(mono)
				.assertError(ServerWebInputException.class);

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveWithName() throws Exception {
		MethodParameter param = initMethodParameter(1);
		Foo foo = new Foo();
		this.exchange.getAttributes().put("specialFoo", foo);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveNotRequired() throws Exception {
		MethodParameter param = initMethodParameter(2);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertNull(mono.block());

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveOptional() throws Exception {
		MethodParameter param = initMethodParameter(3);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);

		assertNotNull(mono.block());
		assertEquals(Optional.class, mono.block().getClass());
		assertFalse(((Optional) mono.block()).isPresent());

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, bindingContext, this.exchange);

		assertNotNull(mono.block());
		assertEquals(Optional.class, mono.block().getClass());
		Optional optional = (Optional) mono.block();
		assertTrue(optional.isPresent());
		assertSame(foo, optional.get());
	}


	private MethodParameter initMethodParameter(int parameterIndex) {
		MethodParameter param = new SynthesizingMethodParameter(this.handleMethod, parameterIndex);
		param.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
		GenericTypeResolver.resolveParameterType(param, this.resolver.getClass());
		return param;
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private void handleWithRequestAttribute(
			@RequestAttribute Foo foo,
			@RequestAttribute("specialFoo") Foo namedFoo,
			@RequestAttribute(name="foo", required = false) Foo notRequiredFoo,
			@RequestAttribute(name="foo") Optional<Foo> optionalFoo,
			String notSupported) {
	}

	private static class Foo {
	}

}

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
import java.net.URI;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.TestSubscriber;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
	@SuppressWarnings("ConfusingArgumentToVarargsMethod")
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ConversionService cs = new DefaultConversionService();
		this.resolver = new SessionAttributeMethodArgumentResolver(cs, context.getBeanFactory());

		this.session = mock(WebSession.class);
		when(this.session.getAttribute(any())).thenReturn(Optional.empty());

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = new MockWebSessionManager(this.session);
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		this.handleMethod = ReflectionUtils.findMethod(getClass(), "handleWithSessionAttribute", (Class<?>[]) null);
	}

	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 0)));
		assertFalse(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 4)));
	}

	@Test
	public void resolve() throws Exception {
		MethodParameter param = initMethodParameter(0);
		Mono<Object> mono = this.resolver.resolveArgument(param, null, this.exchange);
		TestSubscriber
				.subscribe(mono)
				.assertError(ServerWebInputException.class);

		Foo foo = new Foo();
		when(this.session.getAttribute("foo")).thenReturn(Optional.of(foo));
		mono = this.resolver.resolveArgument(param, null, this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveWithName() throws Exception {
		MethodParameter param = initMethodParameter(1);
		Foo foo = new Foo();
		when(this.session.getAttribute("specialFoo")).thenReturn(Optional.of(foo));
		Mono<Object> mono = this.resolver.resolveArgument(param, null, this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveNotRequired() throws Exception {
		MethodParameter param = initMethodParameter(2);
		Mono<Object> mono = this.resolver.resolveArgument(param, null, this.exchange);
		assertNull(mono.block());

		Foo foo = new Foo();
		when(this.session.getAttribute("foo")).thenReturn(Optional.of(foo));
		mono = this.resolver.resolveArgument(param, null, this.exchange);
		assertSame(foo, mono.block());
	}

	@Test
	public void resolveOptional() throws Exception {
		MethodParameter param = initMethodParameter(3);
		Mono<Object> mono = this.resolver.resolveArgument(param, null, this.exchange);
		assertNotNull(mono.block());
		assertEquals(Optional.class, mono.block().getClass());
		assertFalse(((Optional) mono.block()).isPresent());

		Foo foo = new Foo();
		when(this.session.getAttribute("foo")).thenReturn(Optional.of(foo));
		mono = this.resolver.resolveArgument(param, null, this.exchange);

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

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

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebInputException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture with {@link CookieValueMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class CookieValueMethodArgumentResolverTests {

	private CookieValueMethodArgumentResolver resolver;

	private BindingContext bindingContext;

	private MethodParameter cookieParameter;
	private MethodParameter cookieStringParameter;
	private MethodParameter stringParameter;
	private MethodParameter cookieMonoParameter;


	@BeforeEach
	@SuppressWarnings("resource")
	public void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();

		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new CookieValueMethodArgumentResolver(context.getBeanFactory(), adapterRegistry);
		this.bindingContext = new BindingContext();

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		this.cookieParameter = new SynthesizingMethodParameter(method, 0);
		this.cookieStringParameter = new SynthesizingMethodParameter(method, 1);
		this.stringParameter = new SynthesizingMethodParameter(method, 2);
		this.cookieMonoParameter = new SynthesizingMethodParameter(method, 3);
	}


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.cookieParameter)).isTrue();
		assertThat(this.resolver.supportsParameter(this.cookieStringParameter)).isTrue();
	}

	@Test
	public void doesNotSupportParameter() {
		assertThat(this.resolver.supportsParameter(this.stringParameter)).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.cookieMonoParameter))
			.withMessageStartingWith("CookieValueMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void resolveCookieArgument() {
		HttpCookie expected = new HttpCookie("name", "foo");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").cookie(expected));

		Mono<Object> mono = this.resolver.resolveArgument(
				this.cookieParameter, this.bindingContext, exchange);

		assertThat(mono.block()).isEqualTo(expected);
	}

	@Test
	public void resolveCookieStringArgument() {
		HttpCookie cookie = new HttpCookie("name", "foo");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").cookie(cookie));

		Mono<Object> mono = this.resolver.resolveArgument(
				this.cookieStringParameter, this.bindingContext, exchange);

		assertThat(mono.block()).as("Invalid result").isEqualTo(cookie.getValue());
	}

	@Test
	public void resolveCookieDefaultValue() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		Object result = this.resolver.resolveArgument(this.cookieStringParameter, this.bindingContext, exchange).block();

		boolean condition = result instanceof String;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo("bar");
	}

	@Test
	public void notFound() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		Mono<Object> mono = resolver.resolveArgument(this.cookieParameter, this.bindingContext, exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}


	@SuppressWarnings("unused")
	public void params(
			@CookieValue("name") HttpCookie cookie,
			@CookieValue(name = "name", defaultValue = "bar") String cookieString,
			String stringParam,
			@CookieValue Mono<String> monoCookie) {
	}

}

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

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ErrorsMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ErrorsArgumentResolverTests {

	private ErrorsMethodArgumentResolver resolver ;

	private final BindingContext bindingContext = new BindingContext();

	private BindingResult bindingResult;

	private ServerWebExchange exchange;

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {
		this.resolver = new ErrorsMethodArgumentResolver(new ReactiveAdapterRegistry());

		MockServerHttpRequest request = MockServerHttpRequest.post("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response);

		Foo foo = new Foo();
		WebExchangeDataBinder binder = this.bindingContext.createDataBinder(this.exchange, foo, "foo");
		this.bindingResult = binder.getBindingResult();
	}


	@Test
	public void supports() throws Exception {
		MethodParameter parameter = this.testMethod.arg(Errors.class);
		assertTrue(this.resolver.supportsParameter(parameter));

		parameter = this.testMethod.arg(BindingResult.class);
		assertTrue(this.resolver.supportsParameter(parameter));

		parameter = this.testMethod.arg(ResolvableType.forClassWithGenerics(Mono.class, Errors.class));
		assertFalse(this.resolver.supportsParameter(parameter));

		parameter = this.testMethod.arg(String.class);
		assertFalse(this.resolver.supportsParameter(parameter));
	}

	@Test
	public void resolveErrors() throws Exception {
		testResolve(this.bindingResult);
	}

	@Test
	public void resolveErrorsMono() throws Exception {
		MonoProcessor<BindingResult> monoProcessor = MonoProcessor.create();
		monoProcessor.onNext(this.bindingResult);
		testResolve(monoProcessor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolveErrorsAfterMonoModelAttribute() throws Exception {
		MethodParameter parameter = this.testMethod.arg(BindingResult.class);
		this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange).blockMillis(5000);
	}


	private void testResolve(Object bindingResult) {

		String key = BindingResult.MODEL_KEY_PREFIX + "foo";
		this.bindingContext.getModel().asMap().put(key, bindingResult);

		MethodParameter parameter = this.testMethod.arg(Errors.class);

		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.blockMillis(5000);

		assertSame(this.bindingResult, actual);
	}


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
			String string) {}

}

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ErrorsMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class ErrorsMethodArgumentResolverTests {

	private final ErrorsMethodArgumentResolver resolver =
			new ErrorsMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final BindingContext bindingContext = new BindingContext();

	private final MockServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/path"));

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();


	@Test
	public void supports() throws Exception {
		MethodParameter parameter = this.testMethod.arg(Errors.class);
		assertTrue(this.resolver.supportsParameter(parameter));

		parameter = this.testMethod.arg(BindingResult.class);
		assertTrue(this.resolver.supportsParameter(parameter));

		parameter = this.testMethod.arg(ResolvableType.forClassWithGenerics(Mono.class, Errors.class));
		assertTrue(this.resolver.supportsParameter(parameter));

		parameter = this.testMethod.arg(String.class);
		assertFalse(this.resolver.supportsParameter(parameter));
	}

	@Test
	public void resolve() throws Exception {

		BindingResult bindingResult = createBindingResult(new Foo(), "foo");
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "foo", bindingResult);

		MethodParameter parameter = this.testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertSame(bindingResult, actual);
	}

	private BindingResult createBindingResult(Foo target, String name) {
		DataBinder binder = this.bindingContext.createDataBinder(this.exchange, target, name);
		return binder.getBindingResult();
	}

	@Test
	public void resolveWithMono() throws Exception {

		BindingResult bindingResult = createBindingResult(new Foo(), "foo");
		MonoProcessor<BindingResult> monoProcessor = MonoProcessor.create();
		monoProcessor.onNext(bindingResult);
		this.bindingContext.getModel().asMap().put(BindingResult.MODEL_KEY_PREFIX + "foo", monoProcessor);

		MethodParameter parameter = this.testMethod.arg(Errors.class);
		Object actual = this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));

		assertSame(bindingResult, actual);
	}

	@Test
	public void resolveWithMonoOnBindingResultAndModelAttribute() throws Exception {

		this.expectedException.expectMessage("An @ModelAttribute and an Errors/BindingResult) arguments " +
				"cannot both be declared with an async type wrapper.");

		MethodParameter parameter = this.testMethod.arg(BindingResult.class);
		this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));
	}

	@Test // SPR-16187
	public void resolveWithBindingResultNotFound() throws Exception {

		this.expectedException.expectMessage("An Errors/BindingResult argument is expected " +
				"immediately after the @ModelAttribute argument");

		MethodParameter parameter = this.testMethod.arg(Errors.class);
		this.resolver.resolveArgument(parameter, this.bindingContext, this.exchange)
				.block(Duration.ofMillis(5000));
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
			String string) {}

}

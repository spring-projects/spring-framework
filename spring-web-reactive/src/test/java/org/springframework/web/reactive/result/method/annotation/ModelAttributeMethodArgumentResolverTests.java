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

import java.util.Map;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.*;
import static org.springframework.util.Assert.*;


/**
 * Unit tests for {@link ModelAttributeMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class ModelAttributeMethodArgumentResolverTests {

	private ServerWebExchange exchange;

	private final MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.POST, "/path");

	private final MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

	private BindingContext bindingContext;

	private ResolvableMethod testMethod = ResolvableMethod.onClass(this.getClass()).name("handle");


	@Before
	public void setUp() throws Exception {
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, response, new MockWebSessionManager());
		this.exchange = this.exchange.mutate().setFormData(Mono.just(this.formData)).build();

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setValidator(validator);
		this.bindingContext = new BindingContext(initializer);
	}


	@Test
	public void supports() throws Exception {

		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(new ReactiveAdapterRegistry(), false);

		ResolvableType type = forClass(Foo.class);
		assertTrue(resolver.supportsParameter(parameter(type)));

		type = forClassWithGenerics(Mono.class, Foo.class);
		assertTrue(resolver.supportsParameter(parameter(type)));

		type = forClass(Foo.class);
		assertFalse(resolver.supportsParameter(parameterNotAnnotated(type)));

		type = forClassWithGenerics(Mono.class, Foo.class);
		assertFalse(resolver.supportsParameter(parameterNotAnnotated(type)));
	}

	@Test
	public void supportsWithDefaultResolution() throws Exception {

		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(new ReactiveAdapterRegistry(), true);

		ResolvableType type = forClass(Foo.class);
		assertTrue(resolver.supportsParameter(parameterNotAnnotated(type)));

		type = forClassWithGenerics(Mono.class, Foo.class);
		assertTrue(resolver.supportsParameter(parameterNotAnnotated(type)));

		type = forClass(String.class);
		assertFalse(resolver.supportsParameter(parameterNotAnnotated(type)));

		type = forClassWithGenerics(Mono.class, String.class);
		assertFalse(resolver.supportsParameter(parameterNotAnnotated(type)));
	}

	@Test
	public void createAndBind() throws Exception {
		testBindFoo(forClass(Foo.class), value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	@Test
	public void createAndBindToMono() throws Exception {
		testBindFoo(forClassWithGenerics(Mono.class, Foo.class), mono -> {
			assertTrue(mono.getClass().getName(), mono instanceof Mono);
			Object value = ((Mono<?>) mono).blockMillis(5000);
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	@Test
	public void createAndBindToSingle() throws Exception {
		testBindFoo(forClassWithGenerics(Single.class, Foo.class), single -> {
			assertTrue(single.getClass().getName(), single instanceof Single);
			Object value = ((Single<?>) single).toBlocking().value();
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	@Test
	public void bindExisting() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindingContext.getModel().addAttribute(foo);

		testBindFoo(forClass(Foo.class), value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});

		assertSame(foo, this.bindingContext.getModel().asMap().get("foo"));
	}

	@Test
	public void bindExistingMono() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindingContext.getModel().addAttribute("foo", Mono.just(foo));

		testBindFoo(forClass(Foo.class), value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});

		assertSame(foo, this.bindingContext.getModel().asMap().get("foo"));
	}

	@Test
	public void bindExistingSingle() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindingContext.getModel().addAttribute("foo", Single.just(foo));

		testBindFoo(forClass(Foo.class), value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});

		assertSame(foo, this.bindingContext.getModel().asMap().get("foo"));
	}

	@Test
	public void bindExistingMonoToMono() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindingContext.getModel().addAttribute("foo", Mono.just(foo));

		testBindFoo(forClassWithGenerics(Mono.class, Foo.class), mono -> {
			assertTrue(mono.getClass().getName(), mono instanceof Mono);
			Object value = ((Mono<?>) mono).blockMillis(5000);
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	@Test
	public void validationError() throws Exception {
		testValidationError(forClass(Foo.class), resolvedArgumentMono -> resolvedArgumentMono);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validationErrorToMono() throws Exception {
		testValidationError(forClassWithGenerics(Mono.class, Foo.class),
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.blockMillis(5000);
					assertNotNull(value);
					isTrue(value instanceof Mono);
					return (Mono<?>) value;
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validationErrorToSingle() throws Exception {
		testValidationError(forClassWithGenerics(Single.class, Foo.class),
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.blockMillis(5000);
					assertNotNull(value);
					isTrue(value instanceof Single);
					return Mono.from(RxReactiveStreams.toPublisher((Single) value));
				});
	}


	private void testBindFoo(ResolvableType type, Function<Object, Foo> valueExtractor) {

		this.formData.add("name", "Robert");
		this.formData.add("age", "25");

		Object value = createResolver()
				.resolveArgument(parameter(type), this.bindingContext, this.exchange)
				.blockMillis(5000);

		Foo foo = valueExtractor.apply(value);
		assertEquals("Robert", foo.getName());

		String key = "foo";
		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + key;

		Map<String, Object> map = bindingContext.getModel().asMap();
		assertEquals(map.toString(), 2, map.size());
		assertSame(foo, map.get(key));
		assertNotNull(map.get(bindingResultKey));
		assertTrue(map.get(bindingResultKey) instanceof BindingResult);
	}

	private void testValidationError(ResolvableType type, Function<Mono<?>, Mono<?>> valueMonoExtractor) {

		this.formData.add("age", "invalid");

		HandlerMethodArgumentResolver resolver = createResolver();
		Mono<?> mono = resolver.resolveArgument(parameter(type), this.bindingContext, this.exchange);

		mono = valueMonoExtractor.apply(mono);

		StepVerifier.create(mono)
				.consumeErrorWith(ex -> {
					assertTrue(ex instanceof WebExchangeBindException);
					WebExchangeBindException bindException = (WebExchangeBindException) ex;
					assertEquals(1, bindException.getErrorCount());
					assertTrue(bindException.hasFieldErrors("age"));
				})
				.verify();
	}


	private ModelAttributeMethodArgumentResolver createResolver() {
		return new ModelAttributeMethodArgumentResolver(new ReactiveAdapterRegistry());
	}

	private MethodParameter parameter(ResolvableType type) {
		return this.testMethod.resolveParam(type,
				parameter -> parameter.hasParameterAnnotation(ModelAttribute.class));
	}

	private MethodParameter parameterNotAnnotated(ResolvableType type) {
		return this.testMethod.resolveParam(type,
				parameter -> !parameter.hasParameterAnnotations());
	}


	@SuppressWarnings("unused")
	void handle(
			@ModelAttribute @Validated Foo foo,
			@ModelAttribute @Validated Mono<Foo> mono,
			@ModelAttribute @Validated Single<Foo> single,
			Foo fooNotAnnotated,
			String stringNotAnnotated,
			Mono<Foo> monoNotAnnotated,
			Mono<String> monoStringNotAnnotated) {
	}


	private static class Foo {

		private String name;

		private int age;

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

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

}

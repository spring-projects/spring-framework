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

import java.net.URISyntaxException;
import java.time.Duration;
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
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ModelAttributeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class ModelAttributeMethodArgumentResolverTests {

	private BindingContext bindContext;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Before
	public void setup() throws Exception {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setValidator(validator);
		this.bindContext = new BindingContext(initializer);
	}


	@Test
	public void supports() throws Exception {
		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(new ReactiveAdapterRegistry(), false);

		MethodParameter param = this.testMethod.annotPresent(ModelAttribute.class).arg(Foo.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);
		assertFalse(resolver.supportsParameter(param));
	}

	@Test
	public void supportsWithDefaultResolution() throws Exception {
		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(new ReactiveAdapterRegistry(), true);

		MethodParameter param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(String.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, String.class);
		assertFalse(resolver.supportsParameter(param));
	}

	@Test
	public void createAndBind() throws Exception {
		testBindFoo("foo", this.testMethod.annotPresent(ModelAttribute.class).arg(Foo.class), value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	@Test
	public void createAndBindToMono() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);

		testBindFoo("fooMono", parameter, mono -> {
			assertTrue(mono.getClass().getName(), mono instanceof Mono);
			Object value = ((Mono<?>) mono).block(Duration.ofSeconds(5));
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	@Test
	public void createAndBindToSingle() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotPresent(ModelAttribute.class).arg(Single.class, Foo.class);

		testBindFoo("fooSingle", parameter, single -> {
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
		this.bindContext.getModel().addAttribute(foo);

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testBindFoo("foo", parameter, value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});

		assertSame(foo, this.bindContext.getModel().asMap().get("foo"));
	}

	@Test
	public void bindExistingMono() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindContext.getModel().addAttribute("fooMono", Mono.just(foo));

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testBindFoo("foo", parameter, value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});

		assertSame(foo, this.bindContext.getModel().asMap().get("foo"));
	}

	@Test
	public void bindExistingSingle() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindContext.getModel().addAttribute("fooSingle", Single.just(foo));

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testBindFoo("foo", parameter, value -> {
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});

		assertSame(foo, this.bindContext.getModel().asMap().get("foo"));
	}

	@Test
	public void bindExistingMonoToMono() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		String modelKey = "fooMono";
		this.bindContext.getModel().addAttribute(modelKey, Mono.just(foo));

		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);

		testBindFoo(modelKey, parameter, mono -> {
			assertTrue(mono.getClass().getName(), mono instanceof Mono);
			Object value = ((Mono<?>) mono).block(Duration.ofSeconds(5));
			assertEquals(Foo.class, value.getClass());
			return (Foo) value;
		});
	}

	private void testBindFoo(String modelKey, MethodParameter param, Function<Object, Foo> valueExtractor)
			throws Exception {

		Object value = createResolver()
				.resolveArgument(param, this.bindContext, postForm("name=Robert&age=25"))
				.block(Duration.ZERO);

		Foo foo = valueExtractor.apply(value);
		assertEquals("Robert", foo.getName());
		assertEquals(25, foo.getAge());

		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + modelKey;

		Map<String, Object> map = bindContext.getModel().asMap();
		assertEquals(map.toString(), 2, map.size());
		assertSame(foo, map.get(modelKey));
		assertNotNull(map.get(bindingResultKey));
		assertTrue(map.get(bindingResultKey) instanceof BindingResult);
	}

	@Test
	public void validationError() throws Exception {
		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testValidationError(parameter, Function.identity());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validationErrorToMono() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);

		testValidationError(parameter,
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
					assertNotNull(value);
					assertTrue(value instanceof Mono);
					return (Mono<?>) value;
				});
	}

	@Test
	public void validationErrorToSingle() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotPresent(ModelAttribute.class).arg(Single.class, Foo.class);

		testValidationError(parameter,
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
					assertNotNull(value);
					assertTrue(value instanceof Single);
					return Mono.from(RxReactiveStreams.toPublisher((Single<?>) value));
				});
	}

	private void testValidationError(MethodParameter param, Function<Mono<?>, Mono<?>> valueMonoExtractor)
			throws URISyntaxException {

		ServerWebExchange exchange = postForm("age=invalid");
		Mono<?> mono = createResolver().resolveArgument(param, this.bindContext, exchange);
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

	@Test
	public void bindDataClass() throws Exception {
		testBindBar(this.testMethod.annotNotPresent(ModelAttribute.class).arg(Bar.class));
	}

	private void testBindBar(MethodParameter param) throws Exception {
		Object value = createResolver()
				.resolveArgument(param, this.bindContext, postForm("name=Robert&age=25&count=1"))
				.block(Duration.ZERO);

		Bar bar = (Bar) value;
		assertEquals("Robert", bar.getName());
		assertEquals(25, bar.getAge());
		assertEquals(1, bar.getCount());

		String key = "bar";
		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + key;

		Map<String, Object> map = bindContext.getModel().asMap();
		assertEquals(map.toString(), 2, map.size());
		assertSame(bar, map.get(key));
		assertNotNull(map.get(bindingResultKey));
		assertTrue(map.get(bindingResultKey) instanceof BindingResult);
	}


	private ModelAttributeMethodArgumentResolver createResolver() {
		return new ModelAttributeMethodArgumentResolver(new ReactiveAdapterRegistry(), false);
	}

	private ServerWebExchange postForm(String formData) throws URISyntaxException {
		return MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.toExchange();
	}


	@SuppressWarnings("unused")
	void handle(
			@ModelAttribute @Validated Foo foo,
			@ModelAttribute @Validated Mono<Foo> mono,
			@ModelAttribute @Validated Single<Foo> single,
			Foo fooNotAnnotated,
			String stringNotAnnotated,
			Mono<Foo> monoNotAnnotated,
			Mono<String> monoStringNotAnnotated,
			Bar barNotAnnotated) {
	}


	@SuppressWarnings("unused")
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


	private static class Bar {

		private final String name;

		private final int age;

		private int count;

		public Bar(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return this.age;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

}

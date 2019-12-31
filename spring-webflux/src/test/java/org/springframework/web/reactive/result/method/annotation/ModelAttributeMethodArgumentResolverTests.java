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

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ModelAttributeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class ModelAttributeMethodArgumentResolverTests {

	private BindingContext bindContext;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@BeforeEach
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
				new ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false);

		MethodParameter param = this.testMethod.annotPresent(ModelAttribute.class).arg(Foo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);
		assertThat(resolver.supportsParameter(param)).isFalse();
	}

	@Test
	public void supportsWithDefaultResolution() throws Exception {
		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), true);

		MethodParameter param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();
	}

	@Test
	public void createAndBind() throws Exception {
		testBindFoo("foo", this.testMethod.annotPresent(ModelAttribute.class).arg(Foo.class), value -> {
			assertThat(value.getClass()).isEqualTo(Foo.class);
			return (Foo) value;
		});
	}

	@Test
	public void createAndBindToMono() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);

		testBindFoo("fooMono", parameter, mono -> {
			boolean condition = mono instanceof Mono;
			assertThat(condition).as(mono.getClass().getName()).isTrue();
			Object value = ((Mono<?>) mono).block(Duration.ofSeconds(5));
			assertThat(value.getClass()).isEqualTo(Foo.class);
			return (Foo) value;
		});
	}

	@Test
	public void createAndBindToSingle() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotPresent(ModelAttribute.class).arg(Single.class, Foo.class);

		testBindFoo("fooSingle", parameter, single -> {
			boolean condition = single instanceof Single;
			assertThat(condition).as(single.getClass().getName()).isTrue();
			Object value = ((Single<?>) single).toBlocking().value();
			assertThat(value.getClass()).isEqualTo(Foo.class);
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
			assertThat(value.getClass()).isEqualTo(Foo.class);
			return (Foo) value;
		});

		assertThat(this.bindContext.getModel().asMap().get("foo")).isSameAs(foo);
	}

	@Test
	public void bindExistingMono() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindContext.getModel().addAttribute("fooMono", Mono.just(foo));

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testBindFoo("foo", parameter, value -> {
			assertThat(value.getClass()).isEqualTo(Foo.class);
			return (Foo) value;
		});

		assertThat(this.bindContext.getModel().asMap().get("foo")).isSameAs(foo);
	}

	@Test
	public void bindExistingSingle() throws Exception {
		Foo foo = new Foo();
		foo.setName("Jim");
		this.bindContext.getModel().addAttribute("fooSingle", Single.just(foo));

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testBindFoo("foo", parameter, value -> {
			assertThat(value.getClass()).isEqualTo(Foo.class);
			return (Foo) value;
		});

		assertThat(this.bindContext.getModel().asMap().get("foo")).isSameAs(foo);
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
			boolean condition = mono instanceof Mono;
			assertThat(condition).as(mono.getClass().getName()).isTrue();
			Object value = ((Mono<?>) mono).block(Duration.ofSeconds(5));
			assertThat(value.getClass()).isEqualTo(Foo.class);
			return (Foo) value;
		});
	}

	private void testBindFoo(String modelKey, MethodParameter param, Function<Object, Foo> valueExtractor)
			throws Exception {

		Object value = createResolver()
				.resolveArgument(param, this.bindContext, postForm("name=Robert&age=25"))
				.block(Duration.ZERO);

		Foo foo = valueExtractor.apply(value);
		assertThat(foo.getName()).isEqualTo("Robert");
		assertThat(foo.getAge()).isEqualTo(25);

		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + modelKey;

		Map<String, Object> map = bindContext.getModel().asMap();
		assertThat(map.size()).as(map.toString()).isEqualTo(2);
		assertThat(map.get(modelKey)).isSameAs(foo);
		assertThat(map.get(bindingResultKey)).isNotNull();
		boolean condition = map.get(bindingResultKey) instanceof BindingResult;
		assertThat(condition).isTrue();
	}

	@Test
	public void validationError() throws Exception {
		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Foo.class);
		testValidationError(parameter, Function.identity());
	}

	@Test
	public void validationErrorToMono() throws Exception {
		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Foo.class);

		testValidationError(parameter,
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
					assertThat(value).isNotNull();
					boolean condition = value instanceof Mono;
					assertThat(condition).isTrue();
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
					assertThat(value).isNotNull();
					boolean condition = value instanceof Single;
					assertThat(condition).isTrue();
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
					boolean condition = ex instanceof WebExchangeBindException;
					assertThat(condition).isTrue();
					WebExchangeBindException bindException = (WebExchangeBindException) ex;
					assertThat(bindException.getErrorCount()).isEqualTo(1);
					assertThat(bindException.hasFieldErrors("age")).isTrue();
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
		assertThat(bar.getName()).isEqualTo("Robert");
		assertThat(bar.getAge()).isEqualTo(25);
		assertThat(bar.getCount()).isEqualTo(1);

		String key = "bar";
		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + key;

		Map<String, Object> map = bindContext.getModel().asMap();
		assertThat(map.size()).as(map.toString()).isEqualTo(2);
		assertThat(map.get(key)).isSameAs(bar);
		assertThat(map.get(bindingResultKey)).isNotNull();
		boolean condition = map.get(bindingResultKey) instanceof BindingResult;
		assertThat(condition).isTrue();
	}

	// TODO: SPR-15871, SPR-15542


	private ModelAttributeMethodArgumentResolver createResolver() {
		return new ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false);
	}

	private ServerWebExchange postForm(String formData) throws URISyntaxException {
		return MockServerWebExchange.from(MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData));
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


	@SuppressWarnings("unused")
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

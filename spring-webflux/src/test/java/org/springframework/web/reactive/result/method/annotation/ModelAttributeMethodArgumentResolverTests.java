/*
 * Copyright 2002-2024 the original author or authors.
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

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Single;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
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
 * Tests for {@link ModelAttributeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
class ModelAttributeMethodArgumentResolverTests {

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

	private BindingContext bindContext;


	@BeforeEach
	void setup() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setPropertyEditorRegistrar(registry ->
				registry.registerCustomEditor(String.class, "name", new StringTrimmerEditor(true)));
		initializer.setValidator(validator);
		this.bindContext = new BindingContext(initializer);
	}


	@Test
	void supports() {
		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false);

		MethodParameter param = this.testMethod.annotPresent(ModelAttribute.class).arg(Pojo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(ModelAttribute.class).arg(NonBindingPojo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(ModelAttribute.class).arg(Mono.class, Pojo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(ModelAttribute.class).arg(Mono.class, NonBindingPojo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Pojo.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, Pojo.class);
		assertThat(resolver.supportsParameter(param)).isFalse();
	}

	@Test
	void supportsWithDefaultResolution() {
		ModelAttributeMethodArgumentResolver resolver =
				new ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), true);

		MethodParameter param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Pojo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, Pojo.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Mono.class, String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();
	}

	@Test
	void createAndBind() {
		testBindPojo("pojo", this.testMethod.annotPresent(ModelAttribute.class).arg(Pojo.class), value -> {
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});
	}

	@Test
	void createAndBindToMono() {
		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Pojo.class);

		testBindPojo("pojoMono", parameter, mono -> {
			assertThat(mono).isInstanceOf(Mono.class);
			Object value = ((Mono<?>) mono).block(Duration.ofSeconds(5));
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});
	}

	@Test
	void createAndBindToSingle() {
		MethodParameter parameter = this.testMethod
				.annotPresent(ModelAttribute.class).arg(Single.class, Pojo.class);

		testBindPojo("pojoSingle", parameter, single -> {
			assertThat(single).isInstanceOf(Single.class);
			Object value = ((Single<?>) single).blockingGet();
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});
	}

	@Test
	void createButDoNotBind() {
		MethodParameter parameter =
				this.testMethod.annotPresent(ModelAttribute.class).arg(NonBindingPojo.class);

		createButDoNotBindToPojo("nonBindingPojo", parameter, value -> {
			assertThat(value).isInstanceOf(NonBindingPojo.class);
			return (NonBindingPojo) value;
		});
	}

	@Test
	void createButDoNotBindToMono() {
		MethodParameter parameter =
				this.testMethod.annotPresent(ModelAttribute.class).arg(Mono.class, NonBindingPojo.class);

		createButDoNotBindToPojo("nonBindingPojoMono", parameter, value -> {
			assertThat(value).isInstanceOf(Mono.class);
			Object extractedValue = ((Mono<?>) value).block(Duration.ofSeconds(5));
			assertThat(extractedValue).isInstanceOf(NonBindingPojo.class);
			return (NonBindingPojo) extractedValue;
		});
	}

	@Test
	void createButDoNotBindToSingle() {
		MethodParameter parameter =
				this.testMethod.annotPresent(ModelAttribute.class).arg(Single.class, NonBindingPojo.class);

		createButDoNotBindToPojo("nonBindingPojoSingle", parameter, value -> {
			assertThat(value).isInstanceOf(Single.class);
			Object extractedValue = ((Single<?>) value).blockingGet();
			assertThat(extractedValue).isInstanceOf(NonBindingPojo.class);
			return (NonBindingPojo) extractedValue;
		});
	}

	private void createButDoNotBindToPojo(String modelKey, MethodParameter methodParameter,
			Function<Object, NonBindingPojo> valueExtractor) {

		Object value = createResolver()
				.resolveArgument(methodParameter, this.bindContext, postForm("name=Enigma"))
				.block(Duration.ZERO);

		NonBindingPojo nonBindingPojo = valueExtractor.apply(value);
		assertThat(nonBindingPojo).isNotNull();
		assertThat(nonBindingPojo.getName()).isNull();

		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + modelKey;

		Map<String, Object> model = bindContext.getModel().asMap();
		assertThat(model).hasSize(2);
		assertThat(model.get(modelKey)).isSameAs(nonBindingPojo);
		assertThat(model.get(bindingResultKey)).isInstanceOf(BindingResult.class);
	}

	@Test
	void bindExisting() {
		Pojo pojo = new Pojo();
		pojo.setName("Jim");
		this.bindContext.getModel().addAttribute(pojo);

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Pojo.class);
		testBindPojo("pojo", parameter, value -> {
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});

		assertThat(this.bindContext.getModel().asMap().get("pojo")).isSameAs(pojo);
	}

	@Test
	void bindExistingMono() {
		Pojo pojo = new Pojo();
		pojo.setName("Jim");
		this.bindContext.getModel().addAttribute("pojoMono", Mono.just(pojo));

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Pojo.class);
		testBindPojo("pojo", parameter, value -> {
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});

		assertThat(this.bindContext.getModel().asMap().get("pojo")).isSameAs(pojo);
	}

	@Test
	void bindExistingSingle() {
		Pojo pojo = new Pojo();
		pojo.setName("Jim");
		this.bindContext.getModel().addAttribute("pojoSingle", Single.just(pojo));

		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Pojo.class);
		testBindPojo("pojo", parameter, value -> {
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});

		assertThat(this.bindContext.getModel().asMap().get("pojo")).isSameAs(pojo);
	}

	@Test
	void bindExistingMonoToMono() {
		Pojo pojo = new Pojo();
		pojo.setName("Jim");
		String modelKey = "pojoMono";
		this.bindContext.getModel().addAttribute(modelKey, Mono.just(pojo));

		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Pojo.class);

		testBindPojo(modelKey, parameter, mono -> {
			assertThat(mono).isInstanceOf(Mono.class);
			Object value = ((Mono<?>) mono).block(Duration.ofSeconds(5));
			assertThat(value.getClass()).isEqualTo(Pojo.class);
			return (Pojo) value;
		});
	}

	private void testBindPojo(String modelKey, MethodParameter param, Function<Object, Pojo> valueExtractor) {

		Object value = createResolver()
				.resolveArgument(param, this.bindContext, postForm("name= Robert&age=25"))
				.block(Duration.ZERO);

		Pojo pojo = valueExtractor.apply(value);
		assertThat(pojo.getName()).isEqualTo("Robert");
		assertThat(pojo.getAge()).isEqualTo(25);

		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + modelKey;

		Map<String, Object> model = bindContext.getModel().asMap();
		assertThat(model).hasSize(2);
		assertThat(model.get(modelKey)).isSameAs(pojo);
		assertThat(model.get(bindingResultKey)).isInstanceOf(BindingResult.class);
	}

	@Test
	void validationErrorForPojo() {
		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(Pojo.class);
		testValidationError(parameter, Function.identity());
	}

	@Test
	void validationErrorForDataClass() {
		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(DataClass.class);
		testValidationError(parameter, Function.identity());
	}

	@Test
	void validationErrorForMono() {
		MethodParameter parameter = this.testMethod
				.annotNotPresent(ModelAttribute.class).arg(Mono.class, Pojo.class);

		testValidationError(parameter,
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
					assertThat(value).isInstanceOf(Mono.class);
					return (Mono<?>) value;
				});
	}

	@Test
	void validationErrorForSingle() {
		MethodParameter parameter = this.testMethod
				.annotPresent(ModelAttribute.class).arg(Single.class, Pojo.class);

		testValidationError(parameter,
				resolvedArgumentMono -> {
					Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
					assertThat(value).isInstanceOf(Single.class);
					return Mono.from(((Single<?>) value).toFlowable());
				});
	}

	@Test
	void validationErrorWithoutBindingForPojo() {
		MethodParameter parameter = this.testMethod.annotPresent(ModelAttribute.class).arg(ValidatedPojo.class);
		testValidationErrorWithoutBinding(parameter, Function.identity());
	}

	@Test
	void validationErrorWithoutBindingForMono() {
		MethodParameter parameter = this.testMethod.annotPresent(ModelAttribute.class).arg(Mono.class, ValidatedPojo.class);

		testValidationErrorWithoutBinding(parameter, resolvedArgumentMono -> {
			Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
			assertThat(value).isInstanceOf(Mono.class);
			return (Mono<?>) value;
		});
	}

	@Test
	void validationErrorWithoutBindingForSingle() {
		MethodParameter parameter = this.testMethod.annotPresent(ModelAttribute.class).arg(Single.class, ValidatedPojo.class);

		testValidationErrorWithoutBinding(parameter, resolvedArgumentMono -> {
			Object value = resolvedArgumentMono.block(Duration.ofSeconds(5));
			assertThat(value).isInstanceOf(Single.class);
			return Mono.from(((Single<?>) value).toFlowable());
		});
	}

	private void testValidationError(MethodParameter parameter, Function<Mono<?>, Mono<?>> valueMonoExtractor) {
		testValidationError(parameter, valueMonoExtractor, "age=invalid", "age", "invalid");
	}

	private void testValidationErrorWithoutBinding(MethodParameter parameter, Function<Mono<?>, Mono<?>> valueMonoExtractor) {
		testValidationError(parameter, valueMonoExtractor, "name=Enigma", "name", null);
	}

	private void testValidationError(MethodParameter param, Function<Mono<?>, Mono<?>> valueMonoExtractor,
			String formData, String field, String rejectedValue) {

		Mono<?> mono = createResolver().resolveArgument(param, this.bindContext, postForm(formData));
		mono = valueMonoExtractor.apply(mono);

		StepVerifier.create(mono)
				.consumeErrorWith(ex -> {
					assertThat(ex).isInstanceOf(WebExchangeBindException.class);
					WebExchangeBindException bindException = (WebExchangeBindException) ex;
					assertThat(bindException.getErrorCount()).isEqualTo(1);
					assertThat(bindException.hasFieldErrors(field)).isTrue();
					assertThat(bindException.getFieldError(field).getRejectedValue()).isEqualTo(rejectedValue);
				})
				.verify();
	}

	@Test
	void bindDataClass() {
		MethodParameter parameter = this.testMethod.annotNotPresent(ModelAttribute.class).arg(DataClass.class);

		Object value = createResolver()
				.resolveArgument(parameter, this.bindContext, postForm("name= Robert&age=25&count=1"))
				.block(Duration.ZERO);

		DataClass dataClass = (DataClass) value;
		assertThat(dataClass.getName()).isEqualTo("Robert");
		assertThat(dataClass.getAge()).isEqualTo(25);
		assertThat(dataClass.getCount()).isEqualTo(1);

		String modelKey = "dataClass";
		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + modelKey;

		Map<String, Object> model = bindContext.getModel().asMap();
		assertThat(model).hasSize(2);
		assertThat(model.get(modelKey)).isSameAs(dataClass);
		assertThat(model.get(bindingResultKey)).isInstanceOf(BindingResult.class);
	}

	// TODO: SPR-15871, SPR-15542


	private ModelAttributeMethodArgumentResolver createResolver() {
		return new ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance(), false);
	}

	private ServerWebExchange postForm(String formData) {
		return MockServerWebExchange.from(MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData));
	}


	@SuppressWarnings("unused")
	void handle(
			@ModelAttribute @Validated Pojo pojo,
			@ModelAttribute @Validated Mono<Pojo> mono,
			@ModelAttribute @Validated Single<Pojo> single,
			@ModelAttribute(binding = false) NonBindingPojo nonBindingPojo,
			@ModelAttribute(binding = false) Mono<NonBindingPojo> monoNonBindingPojo,
			@ModelAttribute(binding = false) Single<NonBindingPojo> singleNonBindingPojo,
			@ModelAttribute(binding = false) @Validated ValidatedPojo validatedPojo,
			@ModelAttribute(binding = false) @Validated Mono<ValidatedPojo> monoValidatedPojo,
			@ModelAttribute(binding = false) @Validated Single<ValidatedPojo> singleValidatedPojo,
			Pojo pojoNotAnnotated,
			String stringNotAnnotated,
			Mono<Pojo> monoNotAnnotated,
			Mono<String> monoStringNotAnnotated,
			DataClass dataClassNotAnnotated) {
	}


	@SuppressWarnings("unused")
	private static class Pojo {

		private String name;

		private int age;

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
	private static class NonBindingPojo {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "NonBindingPojo [name=" + name + "]";
		}
	}


	@SuppressWarnings("unused")
	private static class ValidatedPojo {

		@NotEmpty
		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "ValidatedPojo [name=" + name + "]";
		}
	}


	@SuppressWarnings("unused")
	private static class DataClass {

		private final String name;

		private final int age;

		private int count;

		public DataClass(String name, int age) {
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

/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Method validation tests for Spring MVC controller methods.
 *
 * <p>When adding tests, consider the following others:
 * <ul>
 * <li>{@code HandlerMethodTests} -- detection if methods need validation
 * <li>{@code MethodValidationAdapterTests} -- method validation independent of Spring MVC
 * <li>{@code MethodValidationProxyTests} -- method validation with proxy scenarios
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
public class MethodValidationTests {

	private static final Person mockPerson = mock(Person.class);

	private static final Errors mockErrors = mock(Errors.class);


	private RequestMappingHandlerAdapter handlerAdapter;

	private InvocationCountingValidator jakartaValidator;


	@BeforeEach
	void setup() throws Exception {
		LocaleContextHolder.setDefaultLocale(Locale.UK);

		LocalValidatorFactoryBean validatorBean = new LocalValidatorFactoryBean();
		validatorBean.afterPropertiesSet();
		this.jakartaValidator = new InvocationCountingValidator(validatorBean);
		this.handlerAdapter = initHandlerAdapter(this.jakartaValidator);
	}

	private static RequestMappingHandlerAdapter initHandlerAdapter(Validator validator) throws Exception {
		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setValidator(validator);

		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();

		RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
		handlerAdapter.setWebBindingInitializer(bindingInitializer);
		handlerAdapter.setApplicationContext(context);
		handlerAdapter.afterPropertiesSet();
		return handlerAdapter;
	}

	@AfterEach
	void reset() {
		LocaleContextHolder.setDefaultLocale(null);
	}


	@Test
	void modelAttribute() {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(mockPerson));
		ServerWebExchange exchange = MockServerWebExchange.from(request().queryParam("name", "name=Faustino1234"));

		StepVerifier.create(this.handlerAdapter.handle(exchange, hm))
				.consumeErrorWith(throwable -> {
					WebExchangeBindException ex = (WebExchangeBindException) throwable;

					assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
					assertThat(this.jakartaValidator.getMethodValidationCount()).as("Method validation unexpected").isEqualTo(0);

					assertBeanResult(ex.getBindingResult(), "student", Collections.singletonList(
							"""
						Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
						codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
						arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
						codes [student.name,name]; arguments []; default message [name],10,1]; \
						default message [size must be between 1 and 10]"""));
				})
				.verify();
	}

	@Test
	@SuppressWarnings("unchecked")
	void modelAttributeAsync() {

		// 1 for Mono argument validation + 1 for method validation of @RequestHeader
		this.jakartaValidator.setMaxInvocationsExpected(2);

		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handleAsync(Mono.empty(), ""));

		ServerWebExchange exchange = MockServerWebExchange.from(
				request().queryParam("name", "name=Faustino1234").header("myHeader", "12345"));

		HandlerResult handlerResult = this.handlerAdapter.handle(exchange, hm).block();

		StepVerifier.create(((Mono<String>) handlerResult.getReturnValue()))
				.consumeErrorWith(throwable -> {
					WebExchangeBindException ex = (WebExchangeBindException) throwable;

					assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(2);
					assertThat(this.jakartaValidator.getMethodValidationCount()).isEqualTo(1);

					assertBeanResult(ex.getBindingResult(), "student", Collections.singletonList(
							"""
						Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
						codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
						arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
						codes [student.name,name]; arguments []; default message [name],10,1]; \
						default message [size must be between 1 and 10]"""));
				})
				.verify();
	}

	@Test
	void modelAttributeWithBindingResult() {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(mockPerson, mockErrors));
		ServerWebExchange exchange = MockServerWebExchange.from(request().queryParam("name", "name=Faustino1234"));

		HandlerResult handlerResult = this.handlerAdapter.handle(exchange, hm).block();

		assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(this.jakartaValidator.getMethodValidationCount()).as("Method validation unexpected").isEqualTo(0);

		assertThat(handlerResult.getReturnValue()).isEqualTo(
				"""
			org.springframework.validation.BeanPropertyBindingResult: 1 errors
			Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
			codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [student.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]""");
	}

	@Test
	void modelAttributeWithBindingResultAndRequestHeader() {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(mockPerson, mockErrors, ""));

		ServerWebExchange exchange = MockServerWebExchange.from(
				request().queryParam("name", "name=Faustino1234").header("myHeader", "123"));

		StepVerifier.create(this.handlerAdapter.handle(exchange, hm))
				.consumeErrorWith(throwable -> {
					HandlerMethodValidationException ex = (HandlerMethodValidationException) throwable;

					assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
					assertThat(this.jakartaValidator.getMethodValidationCount()).isEqualTo(1);

					assertThat(ex.getAllValidationResults()).hasSize(2);

					assertBeanResult(ex.getBeanResults().get(0), "student", Collections.singletonList(
							"""
						Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
						codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
						arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
						codes [student.name,name]; arguments []; default message [name],10,1]; \
						default message [size must be between 1 and 10]"""));

					assertValueResult(ex.getValueResults().get(0), 2, "123", Collections.singletonList(
							"""
						org.springframework.context.support.DefaultMessageSourceResolvable: \
						codes [Size.validController#handle.myHeader,Size.myHeader,Size.java.lang.String,Size]; \
						arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
						codes [validController#handle.myHeader,myHeader]; arguments []; default message [myHeader],10,5]; \
						default message [size must be between 5 and 10]"""
					));
				})
				.verify();
	}

	@Test
	void validatedWithMethodValidation() {

		// 1 for @Validated argument validation + 1 for method validation of @RequestHeader
		this.jakartaValidator.setMaxInvocationsExpected(2);

		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handleValidated(mockPerson, mockErrors, ""));

		ServerWebExchange exchange = MockServerWebExchange.from(
				request().queryParam("name", "name=Faustino1234").header("myHeader", "12345"));

		HandlerResult handlerResult = this.handlerAdapter.handle(exchange, hm).block();

		assertThat(jakartaValidator.getValidationCount()).isEqualTo(2);
		assertThat(jakartaValidator.getMethodValidationCount()).isEqualTo(1);

		assertThat(handlerResult.getReturnValue()).isEqualTo(
				"""
			org.springframework.validation.BeanPropertyBindingResult: 1 errors
			Field error in object 'person' on field 'name': rejected value [name=Faustino1234]; \
			codes [Size.person.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [person.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]""");
	}

	@Test
	void jakartaAndSpringValidator() {
		HandlerMethod hm = handlerMethod(new InitBinderController(), ibc -> ibc.handle(mockPerson, mockErrors, ""));

		ServerWebExchange exchange = MockServerWebExchange.from(
				request().queryParam("name", "name=Faustino1234").header("myHeader", "12345"));

		HandlerResult handlerResult = this.handlerAdapter.handle(exchange, hm).block();

		assertThat(jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(jakartaValidator.getMethodValidationCount()).isEqualTo(1);

		assertThat(handlerResult.getReturnValue()).isEqualTo(
				"""
			org.springframework.validation.BeanPropertyBindingResult: 2 errors
			Field error in object 'person' on field 'name': rejected value [name=Faustino1234]; \
			codes [TOO_LONG.person.name,TOO_LONG.name,TOO_LONG.java.lang.String,TOO_LONG]; \
			arguments []; default message [length must be 10 or under]
			Field error in object 'person' on field 'name': rejected value [name=Faustino1234]; \
			codes [Size.person.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [person.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]""");
	}


	@Test
	void springValidator() throws Exception {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(mockPerson, mockErrors));
		ServerWebExchange exchange = MockServerWebExchange.from(request().queryParam("name", "name=Faustino1234"));

		RequestMappingHandlerAdapter springValidatorHandlerAdapter = initHandlerAdapter(new PersonValidator());
		HandlerResult handlerResult = springValidatorHandlerAdapter.handle(exchange, hm).block();

		assertThat(handlerResult.getReturnValue()).isEqualTo(
				"""
			org.springframework.validation.BeanPropertyBindingResult: 1 errors
			Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
			codes [TOO_LONG.student.name,TOO_LONG.name,TOO_LONG.java.lang.String,TOO_LONG]; \
			arguments []; default message [length must be 10 or under]""");
	}


	@SuppressWarnings("unchecked")
	private static <T> HandlerMethod handlerMethod(T controller, Consumer<T> mockCallConsumer) {
		Method method = ResolvableMethod.on((Class<T>) controller.getClass()).mockCall(mockCallConsumer).method();
		return new HandlerMethod(controller, method);
	}

	private static MockServerHttpRequest.BodyBuilder request() {
		return MockServerHttpRequest.post("").contentType(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@SuppressWarnings("SameParameterValue")
	private static void assertBeanResult(Errors errors, String objectName, List<String> fieldErrors) {
		assertThat(errors.getObjectName()).isEqualTo(objectName);
		assertThat(errors.getFieldErrors())
				.extracting(FieldError::toString)
				.containsExactlyInAnyOrderElementsOf(fieldErrors);
	}

	@SuppressWarnings("SameParameterValue")
	private static void assertValueResult(
			ParameterValidationResult result, int parameterIndex, Object argument, List<String> errors) {

		assertThat(result.getMethodParameter().getParameterIndex()).isEqualTo(parameterIndex);
		assertThat(result.getArgument()).isEqualTo(argument);
		assertThat(result.getResolvableErrors())
				.extracting(MessageSourceResolvable::toString)
				.containsExactlyInAnyOrderElementsOf(errors);
	}


	@SuppressWarnings("unused")
	private record Person(@Size(min = 1, max = 10) String name) {

		@Override
		public String name() {
			return this.name;
		}
	}


	@SuppressWarnings({"unused", "SameParameterValue", "UnusedReturnValue"})
	@RestController
	static class ValidController {

		void handle(@Valid @ModelAttribute("student") Person person) {
		}

		String handle(@Valid @ModelAttribute("student") Person person, Errors errors) {
			return errors.toString();
		}

		void handle(@Valid @ModelAttribute("student") Person person, Errors errors,
				@RequestHeader @Size(min = 5, max = 10) String myHeader) {
		}

		String handleValidated(@Validated Person person, Errors errors,
				@RequestHeader @Size(min = 5, max = 10) String myHeader) {

			return errors.toString();
		}

		Mono<String> handleAsync(@Valid @ModelAttribute("student") Mono<Person> person,
				@RequestHeader @Size(min = 5, max = 10) String myHeader) {

			return person.map(Person::toString);
		}
	}


	@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue"})
	@RestController
	static class InitBinderController {

		@InitBinder
		void initBinder(WebDataBinder dataBinder) {
			dataBinder.addValidators(new PersonValidator());
		}

		String handle(@Valid Person person, Errors errors, @RequestHeader @Size(min = 5, max = 10) String myHeader) {
			return errors.toString();
		}
	}


	private static class PersonValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return (clazz == Person.class);
		}

		@Override
		public void validate(Object target, Errors errors) {
			Person person = (Person) target;
			if (person.name().length() > 10) {
				errors.rejectValue("name", "TOO_LONG", "length must be 10 or under");
			}
		}
	}


	/**
	 * Intercept and count number of method validation calls.
	 */
	private static class InvocationCountingValidator implements jakarta.validation.Validator, Validator {

		private final SpringValidatorAdapter delegate;

		private int maxInvocationsExpected = 1;

		private int validationCount;

		private int methodValidationCount;

		/**
		 * Constructor with maxCount=1.
		 */
		private InvocationCountingValidator(SpringValidatorAdapter delegate) {
			this.delegate = delegate;
		}

		public void setMaxInvocationsExpected(int maxInvocationsExpected) {
			this.maxInvocationsExpected = maxInvocationsExpected;
		}

		/**
		 * Total number of times Bean Validation was invoked.
		 */
		public int getValidationCount() {
			return this.validationCount;
		}

		/**
		 * Number of times method level Bean Validation was invoked.
		 */
		public int getMethodValidationCount() {
			return this.methodValidationCount;
		}

		@Override
		public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
			throw new UnsupportedOperationException();
		}

		@Override
		public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T unwrap(Class<T> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ExecutableValidator forExecutables() {
			this.methodValidationCount++;
			assertCountAndIncrement();
			return this.delegate.forExecutables();
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(Object target, Errors errors) {
			assertCountAndIncrement();
			this.delegate.validate(target, errors);
		}

		private void assertCountAndIncrement() {
			assertThat(this.validationCount++).as("Too many calls to Bean Validation").isLessThan(this.maxInvocationsExpected);
		}
	}

}

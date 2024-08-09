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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

/**
 * Method validation tests for Spring MVC controller methods.
 * <p>When adding tests, consider the following others:
 * <ul>
 * <li>{@code HandlerMethodTests} -- detection if methods need validation
 * <li>{@code MethodValidationAdapterTests} -- method validation independent of Spring MVC
 * <li>{@code MethodValidationProxyTests} -- method validation with proxy scenarios
 * </ul>
 * @author Rossen Stoyanchev
 */
class MethodValidationTests {

	private static final Person mockPerson = mock(Person.class);

	private static final Errors mockErrors = mock(Errors.class);


	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private RequestMappingHandlerAdapter handlerAdapter;

	private InvocationCountingValidator jakartaValidator;


	@BeforeEach
	void setup() throws Exception {
		LocalValidatorFactoryBean validatorBean = new LocalValidatorFactoryBean();
		validatorBean.afterPropertiesSet();
		this.jakartaValidator = new InvocationCountingValidator(validatorBean);

		this.handlerAdapter = initHandlerAdapter(this.jakartaValidator);

		this.request.setMethod("POST");
		this.request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		this.request.addHeader("Accept", "text/plain");
		this.request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, new HashMap<>(0));
	}

	private static RequestMappingHandlerAdapter initHandlerAdapter(Validator validator) {
		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setValidator(validator);

		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();

		RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
		handlerAdapter.setWebBindingInitializer(bindingInitializer);
		handlerAdapter.setApplicationContext(context);
		handlerAdapter.setBeanFactory(context.getBeanFactory());
		handlerAdapter.setMessageConverters(
				List.of(new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter()));
		handlerAdapter.afterPropertiesSet();
		return handlerAdapter;
	}


	@Test
	void modelAttribute() {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(mockPerson));
		this.request.addParameter("name", "name=Faustino1234");

		MethodArgumentNotValidException ex = catchThrowableOfType(MethodArgumentNotValidException.class,
				() -> this.handlerAdapter.handle(this.request, this.response, hm));

		assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(this.jakartaValidator.getMethodValidationCount()).as("Method validation unexpected").isEqualTo(0);

		assertBeanResult(ex.getBindingResult(), "student", List.of("""
			Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
			codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [student.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]"""
		));
	}

	@Test
	void modelAttributeWithBindingResult() throws Exception {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(mockPerson, mockErrors));
		this.request.addParameter("name", "name=Faustino1234");

		this.handlerAdapter.handle(this.request, this.response, hm);

		assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(this.jakartaValidator.getMethodValidationCount()).as("Method validation unexpected").isEqualTo(0);

		assertThat(response.getContentAsString()).isEqualTo("""
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
		this.request.addParameter("name", "name=Faustino1234");
		this.request.addHeader("myHeader", "123");

		HandlerMethodValidationException ex = catchThrowableOfType(HandlerMethodValidationException.class,
				() -> this.handlerAdapter.handle(this.request, this.response, hm));

		assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(this.jakartaValidator.getMethodValidationCount()).isEqualTo(1);

		assertThat(ex.getParameterValidationResults()).hasSize(2);

		assertBeanResult(ex.getBeanResults().get(0), "student", List.of("""
			Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
			codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [student.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]"""
		));

		assertValueResult(ex.getValueResults().get(0), 2, "123", List.of("""
			org.springframework.validation.beanvalidation.MethodValidationAdapter$ViolationMessageSourceResolvable: \
			codes [Size.validController#handle.myHeader,Size.myHeader,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [validController#handle.myHeader,myHeader]; arguments []; default message [myHeader],10,5]; \
			default message [size must be between 5 and 10]"""
		));
	}

	@Test
	void validatedWithMethodValidation() throws Exception {

		// 1 for @Validated argument validation + 1 for method validation of @RequestHeader
		this.jakartaValidator.setMaxInvocationsExpected(2);

		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handleValidated(mockPerson, mockErrors, ""));
		this.request.addParameter("name", "name=Faustino1234");
		this.request.addHeader("myHeader", "12345");

		this.handlerAdapter.handle(this.request, this.response, hm);

		assertThat(jakartaValidator.getValidationCount()).isEqualTo(2);
		assertThat(jakartaValidator.getMethodValidationCount()).isEqualTo(1);

		assertThat(response.getContentAsString()).isEqualTo("""
			org.springframework.validation.BeanPropertyBindingResult: 1 errors
			Field error in object 'person' on field 'name': rejected value [name=Faustino1234]; \
			codes [Size.person.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [person.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]""");
	}

	@Test
	void validateList() {
		HandlerMethod hm = handlerMethod(new ValidController(), c -> c.handle(List.of(mockPerson, mockPerson)));
		this.request.setContentType(MediaType.APPLICATION_JSON_VALUE);
		this.request.setContent("[{\"name\":\"Faustino1234\"},{\"name\":\"Cayetana6789\"}]".getBytes(UTF_8));

		HandlerMethodValidationException ex = catchThrowableOfType(HandlerMethodValidationException.class,
				() -> this.handlerAdapter.handle(this.request, this.response, hm));

		assertThat(this.jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(this.jakartaValidator.getMethodValidationCount()).isEqualTo(1);

		assertThat(ex.getParameterValidationResults()).hasSize(2);

		assertBeanResult(ex.getBeanResults().get(0), "personList", List.of("""
			Field error in object 'personList' on field 'name': rejected value [Faustino1234]; \
			codes [Size.personList.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [personList.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]"""
		));

		assertBeanResult(ex.getBeanResults().get(1), "personList", List.of("""
			Field error in object 'personList' on field 'name': rejected value [Cayetana6789]; \
			codes [Size.personList.name,Size.name,Size.java.lang.String,Size]; \
			arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
			codes [personList.name,name]; arguments []; default message [name],10,1]; \
			default message [size must be between 1 and 10]"""
		));

	}

	@Test
	void jakartaAndSpringValidator() throws Exception {
		HandlerMethod hm = handlerMethod(new InitBinderController(), ibc -> ibc.handle(mockPerson, mockErrors, ""));
		this.request.addParameter("name", "name=Faustino1234");
		this.request.addHeader("myHeader", "12345");

		this.handlerAdapter.handle(this.request, this.response, hm);

		assertThat(jakartaValidator.getValidationCount()).isEqualTo(1);
		assertThat(jakartaValidator.getMethodValidationCount()).isEqualTo(1);

		assertThat(response.getContentAsString()).isEqualTo("""
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
		this.request.addParameter("name", "name=Faustino1234");

		RequestMappingHandlerAdapter springValidatorHandlerAdapter = initHandlerAdapter(new PersonValidator());
		springValidatorHandlerAdapter.handle(this.request, this.response, hm);

		assertThat(response.getContentAsString()).isEqualTo("""
			org.springframework.validation.BeanPropertyBindingResult: 1 errors
			Field error in object 'student' on field 'name': rejected value [name=Faustino1234]; \
			codes [TOO_LONG.student.name,TOO_LONG.name,TOO_LONG.java.lang.String,TOO_LONG]; \
			arguments []; default message [length must be 10 or under]""");
	}


	@SuppressWarnings("unchecked")
	private static <T> HandlerMethod handlerMethod(T controller, Consumer<T> mockCallConsumer) {
		Method method = ResolvableMethod.on((Class<T>) controller.getClass()).mockCall(mockCallConsumer).method();
		return new HandlerMethod(controller, method).createWithValidateFlags();
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
	private record Person(@Size(min = 1, max = 10) @JsonProperty("name") String name) {

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

		void handle(@Valid @RequestBody List<Person> persons) {
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

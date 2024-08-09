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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraintvalidation.SupportedValidationTarget;
import jakarta.validation.constraintvalidation.ValidationTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MethodValidationAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class MethodValidationAdapterTests {

	private static final Person faustino1234 = new Person("Faustino1234", List.of("Working on Spring"));

	private static final Person cayetana6789 = new Person("Cayetana6789", List.of("  "));


	private final MethodValidationAdapter validationAdapter = new MethodValidationAdapter();

	private final Locale originalLocale = Locale.getDefault();


	@BeforeEach
	void setDefaultLocaleToEnglish() {
		Locale.setDefault(Locale.ENGLISH);
	}

	@AfterEach
	void resetDefaultLocale() {
		Locale.setDefault(this.originalLocale);
	}

	@Test
	void validateArguments() {
		MyService target = new MyService();
		Method method = getMethod(target, "addStudent");

		testArgs(target, method, new Object[] {faustino1234, cayetana6789, 3}, ex -> {

			assertThat(ex.getParameterValidationResults()).hasSize(3);

			assertBeanResult(ex.getBeanResults().get(0), 0, "student", faustino1234, List.of("""
				Field error in object 'student' on field 'name': rejected value [Faustino1234]; \
				codes [Size.student.name,Size.name,Size.java.lang.String,Size]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [student.name,name]; arguments []; default message [name],10,1]; \
				default message [size must be between 1 and 10]"""));

			assertBeanResult(ex.getBeanResults().get(1), 1, "guardian", cayetana6789, List.of("""
				Field error in object 'guardian' on field 'name': rejected value [Cayetana6789]; \
				codes [Size.guardian.name,Size.name,Size.java.lang.String,Size]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [guardian.name,name]; arguments []; default message [name],10,1]; \
				default message [size must be between 1 and 10]""", """
				Field error in object 'guardian' on field 'hobbies[0]': rejected value [  ]; \
				codes [NotBlank.guardian.hobbies[0],NotBlank.guardian.hobbies,NotBlank.hobbies[0],\
				NotBlank.hobbies,NotBlank.java.lang.String,NotBlank]; arguments \
				[org.springframework.context.support.DefaultMessageSourceResolvable: codes \
				[guardian.hobbies[0],hobbies[0]]; arguments []; default message [hobbies[0]]]; \
				default message [must not be blank]"""));

			assertValueResult(ex.getValueResults().get(0), 2, 3, List.of("""
				org.springframework.validation.beanvalidation.MethodValidationAdapter$ViolationMessageSourceResolvable: \
				codes [Max.myService#addStudent.degrees,Max.degrees,Max.int,Max]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [myService#addStudent.degrees,degrees]; arguments []; default message [degrees],2]; \
				default message [must be less than or equal to 2]"""));
		});
	}

	@Test
	void validateArgumentWithCustomObjectName() {
		MyService target = new MyService();
		Method method = getMethod(target, "addStudent");

		this.validationAdapter.setObjectNameResolver((param, value) -> "studentToAdd");

		testArgs(target, method, new Object[] {faustino1234, new Person("Joe", List.of()), 1}, ex -> {

			assertThat(ex.getParameterValidationResults()).hasSize(1);

			assertBeanResult(ex.getBeanResults().get(0), 0, "studentToAdd", faustino1234, List.of("""
				Field error in object 'studentToAdd' on field 'name': rejected value [Faustino1234]; \
				codes [Size.studentToAdd.name,Size.name,Size.java.lang.String,Size]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [studentToAdd.name,name]; arguments []; default message [name],10,1]; \
				default message [size must be between 1 and 10]"""));
		});
	}

	@Test
	void validateReturnValue() {
		MyService target = new MyService();

		testReturnValue(target, getMethod(target, "getIntValue"), 4, ex -> {

			assertThat(ex.getParameterValidationResults()).hasSize(1);

			assertValueResult(ex.getValueResults().get(0), -1, 4, List.of("""
				org.springframework.validation.beanvalidation.MethodValidationAdapter$ViolationMessageSourceResolvable: \
				codes [Min.myService#getIntValue,Min,Min.int]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [myService#getIntValue]; arguments []; default message [],5]; \
				default message [must be greater than or equal to 5]"""));
		});
	}

	@Test
	void validateReturnValueBean() {
		MyService target = new MyService();

		testReturnValue(target, getMethod(target, "getPerson"), faustino1234, ex -> {

			assertThat(ex.getParameterValidationResults()).hasSize(1);

			assertBeanResult(ex.getBeanResults().get(0), -1, "person", faustino1234, List.of("""
				Field error in object 'person' on field 'name': rejected value [Faustino1234]; \
				codes [Size.person.name,Size.name,Size.java.lang.String,Size]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [person.name,name]; arguments []; default message [name],10,1]; \
				default message [size must be between 1 and 10]"""));
		});
	}

	@Test
	void validateBeanListArgument() {
		MyService target = new MyService();
		Method method = getMethod(target, "addPeople");

		testArgs(target, method, new Object[] {List.of(faustino1234, cayetana6789)}, ex -> {

			assertThat(ex.getParameterValidationResults()).hasSize(2);

			int paramIndex = 0;
			String objectName = "people";
			List<ParameterErrors> results = ex.getBeanResults();

			assertBeanResult(results.get(0), paramIndex, objectName, faustino1234, List.of("""
				Field error in object 'people' on field 'name': rejected value [Faustino1234]; \
				codes [Size.people.name,Size.name,Size.java.lang.String,Size]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [people.name,name]; arguments []; default message [name],10,1]; \
				default message [size must be between 1 and 10]"""));

			assertBeanResult(results.get(1), paramIndex, objectName, cayetana6789, List.of("""
				Field error in object 'people' on field 'name': rejected value [Cayetana6789]; \
				codes [Size.people.name,Size.name,Size.java.lang.String,Size]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [people.name,name]; arguments []; default message [name],10,1]; \
				default message [size must be between 1 and 10]""", """
				Field error in object 'people' on field 'hobbies[0]': rejected value [  ]; \
				codes [NotBlank.people.hobbies[0],NotBlank.people.hobbies,NotBlank.hobbies[0],\
				NotBlank.hobbies,NotBlank.java.lang.String,NotBlank]; arguments \
				[org.springframework.context.support.DefaultMessageSourceResolvable: codes \
				[people.hobbies[0],hobbies[0]]; arguments []; default message [hobbies[0]]]; \
				default message [must not be blank]"""));
		});
	}

	@Test
	void validateValueListArgument() {
		MyService target = new MyService();
		Method method = getMethod(target, "addHobbies");

		testArgs(target, method, new Object[] {List.of("   ")}, ex -> {
			assertThat(ex.getParameterValidationResults()).hasSize(1);
			assertValueResult(ex.getValueResults().get(0), 0, "   ", List.of("""
				org.springframework.validation.beanvalidation.MethodValidationAdapter$ViolationMessageSourceResolvable: \
				codes [NotBlank.myService#addHobbies.hobbies,NotBlank.hobbies,NotBlank.java.util.List,NotBlank]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [myService#addHobbies.hobbies,hobbies]; \
				arguments []; default message [hobbies]]; default message [must not be blank]"""));
		});
	}

	@Test // gh-33150
	void validateValueSetArgument() {
		MyService target = new MyService();
		Method method = getMethod(target, "addUniqueHobbies");

		testArgs(target, method, new Object[] {Set.of("test", "   ")}, ex -> {
			assertThat(ex.getParameterValidationResults()).hasSize(1);
			assertValueResult(ex.getValueResults().get(0), 0, Set.of("test", "   "), List.of("""
				org.springframework.validation.beanvalidation.MethodValidationAdapter$ViolationMessageSourceResolvable: \
				codes [NotBlank.myService#addUniqueHobbies.hobbies,NotBlank.hobbies,NotBlank.java.util.Set,NotBlank]; \
				arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
				codes [myService#addUniqueHobbies.hobbies,hobbies]; \
				arguments []; default message [hobbies]]; default message [must not be blank]"""));
		});
	}

	@Test
	void validateCrossParams() {
		MyService target = new MyService();
		Method method = getMethod(target, "addRange");

		testArgs(target, method, new Object[] {90, 50}, ex -> {
			assertThat(ex.getParameterValidationResults()).isEmpty();
			assertThat(ex.getCrossParameterValidationResults()).hasSize(1);
			assertThat(ex.getCrossParameterValidationResults().get(0).toString()).isEqualTo("""
					org.springframework.validation.beanvalidation.MethodValidationAdapter$ViolationMessageSourceResolvable: \
					codes [RangeParams.myService#addRange,RangeParams]; \
					arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
					codes [myService#addRange]; \
					arguments []; default message []]; default message [Invalid range]""");
		});
	}

	private void testArgs(Object target, Method method, Object[] args, Consumer<MethodValidationResult> consumer) {
		consumer.accept(this.validationAdapter.validateArguments(target, method, null, args, new Class<?>[0]));
	}

	private void testReturnValue(Object target, Method method, @Nullable Object value, Consumer<MethodValidationResult> consumer) {
		consumer.accept(this.validationAdapter.validateReturnValue(target, method, null, value, new Class<?>[0]));
	}

	private static void assertBeanResult(
			ParameterErrors errors, int parameterIndex, String objectName, @Nullable Object argument,
			List<String> fieldErrors) {

		assertThat(errors.getMethodParameter().getParameterIndex()).isEqualTo(parameterIndex);
		assertThat(errors.getObjectName()).isEqualTo(objectName);
		assertThat(errors.getArgument()).isSameAs(argument);

		assertThat(errors.getFieldErrors())
				.extracting(FieldError::toString)
				.containsExactlyInAnyOrderElementsOf(fieldErrors);
	}

	private static void assertValueResult(
			ParameterValidationResult result, int parameterIndex, Object argument, List<String> errors) {

		assertThat(result.getMethodParameter().getParameterIndex()).isEqualTo(parameterIndex);
		assertThat(result.getArgument()).isEqualTo(argument);

		List<MessageSourceResolvable> resolvableErrors = result.getResolvableErrors();
		assertThat(resolvableErrors)
				.extracting(MessageSourceResolvable::toString)
				.containsExactlyInAnyOrderElementsOf(errors);

		resolvableErrors.forEach(error ->
				assertThat(result.unwrap(error, ConstraintViolation.class)).isNotNull());
	}

	private static Method getMethod(Object target, String methodName) {
		return ClassUtils.getMethod(target.getClass(), methodName, (Class<?>[]) null);
	}


	@SuppressWarnings("unused")
	private static class MyService {

		public void addStudent(@Valid Person student, @Valid Person guardian, @Max(2) int degrees) {
		}

		@Min(5)
		public int getIntValue() {
			throw new UnsupportedOperationException();
		}

		@Valid
		public Person getPerson() {
			throw new UnsupportedOperationException();
		}

		public void addPeople(@Valid List<Person> people) {
		}

		public void addHobbies(List<@NotBlank String> hobbies) {
		}

		public void addUniqueHobbies(Set<@NotBlank String> hobbies) {
		}

		@RangeParams
		public void addRange(int from, int to) {
		}
	}


	@SuppressWarnings("unused")
	private record Person(@Size(min = 1, max = 10) String name, List<@NotBlank String> hobbies) {
	}


	@Documented
	@Constraint(validatedBy = RangeParamsValidator.class)
	@Target({ CONSTRUCTOR, METHOD })
	@Retention(RUNTIME)
	public @interface RangeParams {

		String message() default "Invalid range";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};

	}


	@SupportedValidationTarget(ValidationTarget.PARAMETERS)
	public static final class RangeParamsValidator implements ConstraintValidator<RangeParams, Object[]> {

		@Override
		public boolean isValid(final Object[] parameters, final ConstraintValidatorContext context) {
			return false;
		}
	}

}

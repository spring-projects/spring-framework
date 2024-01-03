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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test method validation scenarios with cascaded violations on different types
 * of method parameters and return values.
 *
 * @author Rossen Stoyanchev
 */
class MethodValidationAdapterPropertyPathTests {

	private static final Person validPerson = new Person("John");

	private static final Person invalidPerson = new Person("Long John Silver");

	private static final Class<?>[] HINTS = new Class<?>[0];


	private final MethodValidationAdapter validationAdapter = new MethodValidationAdapter();


	@Nested
	class ArgumentTests {

		@Test
		void fieldOfObjectPropertyOfBean() {
			Method method = getMethod("addCourse");
			Object[] args = {new Course("CS 101", invalidPerson, Collections.emptyList())};

			MethodValidationResult result =
					validationAdapter.validateArguments(new MyService(), method, null, args, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, null, null, null, "professor.name", invalidPerson.name());
		}

		@Test
		void fieldOfObjectPropertyOfListElement() {
			Method method = getMethod("addCourseList");
			List<Course> courses = List.of(new Course("CS 101", invalidPerson, Collections.emptyList()));

			MethodValidationResult result = validationAdapter.validateArguments(
					new MyService(), method, null, new Object[] {courses}, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, courses, 0, null, "professor.name", invalidPerson.name());
		}

		@Test
		void fieldOfObjectPropertyOfListElements() {
			Method method = getMethod("addCourseList");
			List<Course> courses = List.of(
					new Course("CS 101", invalidPerson, Collections.emptyList()),
					new Course("CS 102", invalidPerson, Collections.emptyList()));

			MethodValidationResult result = validationAdapter.validateArguments(
					new MyService(), method, null, new Object[] {courses}, HINTS);

			assertThat(result.getAllErrors()).hasSize(2);
			for (int i = 0; i < 2; i++) {
				ParameterErrors errors = result.getBeanResults().get(i);
				assertThat(errors.getContainerIndex()).isEqualTo(i);
				assertThat(errors.getFieldError().getField()).isEqualTo("professor.name");
			}

		}

		@Test
		void fieldOfObjectPropertyUnderListPropertyOfListElement() {
			Method method = getMethod("addCourseList");
			Course cs101 = new Course("CS 101", invalidPerson, Collections.emptyList());
			Course cs201 = new Course("CS 201", validPerson, List.of(cs101));
			Course cs301 = new Course("CS 301", validPerson, List.of(cs201));
			List<Course> courses = List.of(cs301);
			Object[] args = {courses};

			MethodValidationResult result =
					validationAdapter.validateArguments(new MyService(), method, null, args, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);

			assertSingleFieldError(errors, 1, courses, 0, null,
					"requiredCourses[0].requiredCourses[0].professor.name", invalidPerson.name());
		}

		@Test
		void fieldOfObjectPropertyOfArrayElement() {
			Method method = getMethod("addCourseArray");
			Course[] courses = new Course[] {new Course("CS 101", invalidPerson, Collections.emptyList())};

			MethodValidationResult result = validationAdapter.validateArguments(
					new MyService(), method, null, new Object[] {courses}, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, courses, 0, null, "professor.name", invalidPerson.name());
		}

		@Test
		void fieldOfObjectPropertyOfMapValue() {
			Method method = getMethod("addCourseMap");
			Map<String, Course> courses = Map.of("CS 101", new Course("CS 101", invalidPerson, Collections.emptyList()));

			MethodValidationResult result = validationAdapter.validateArguments(
					new MyService(), method, null, new Object[] {courses}, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, courses, null, "CS 101", "professor.name", invalidPerson.name());
		}

		@Test
		void fieldOfObjectPropertyOfOptionalBean() {
			Method method = getMethod("addOptionalCourse");
			Optional<Course> optional = Optional.of(new Course("CS 101", invalidPerson, Collections.emptyList()));
			Object[] args = {optional};

			MethodValidationResult result =
					validationAdapter.validateArguments(new MyService(), method, null, args, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, optional, null, null, "professor.name", invalidPerson.name());
		}

	}


	@Nested
	class ReturnValueTests {

		@Test
		void fieldOfObjectPropertyOfBean() {
			Method method = getMethod("getCourse");
			Course course = new Course("CS 101", invalidPerson, Collections.emptyList());

			MethodValidationResult result =
					validationAdapter.validateReturnValue(new MyService(), method, null, course, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, null, null, null, "professor.name", invalidPerson.name());
		}

		@Test
		void fieldOfObjectPropertyOfListElement() {
			Method method = getMethod("addCourseList");
			List<Course> courses = List.of(new Course("CS 101", invalidPerson, Collections.emptyList()));

			MethodValidationResult result = validationAdapter.validateArguments(
					new MyService(), method, null, new Object[] {courses}, HINTS);

			assertThat(result.getAllErrors()).hasSize(1);
			ParameterErrors errors = result.getBeanResults().get(0);
			assertSingleFieldError(errors, 1, courses, 0, null, "professor.name", invalidPerson.name());
		}

	}


	private void assertSingleFieldError(
			ParameterErrors errors, int errorCount,
			@Nullable Object container, @Nullable Integer index, @Nullable Object key,
			String field, Object rejectedValue) {

		assertThat(errors.getErrorCount()).isEqualTo(errorCount);
		assertThat(errors.getErrorCount()).isEqualTo(1);
		assertThat(errors.getContainer()).isEqualTo(container);
		assertThat(errors.getContainerIndex()).isEqualTo(index);
		assertThat(errors.getContainerKey()).isEqualTo(key);

		FieldError fieldError = errors.getFieldError();
		assertThat(fieldError).isNotNull();
		assertThat(fieldError.getField()).isEqualTo(field);
		assertThat(fieldError.getRejectedValue()).isEqualTo(rejectedValue);
	}


	private static Method getMethod(String methodName) {
		return ClassUtils.getMethod(MyService.class, methodName, (Class<?>[]) null);
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private static class MyService {

		public void addCourse(@Valid Course course) {
		}

		public void addCourseList(@Valid List<Course> courses) {
		}

		public void addCourseArray(@Valid Course[] courses) {
		}

		public void addCourseMap(@Valid Map<String, Course> courses) {
		}

		public void addOptionalCourse(@Valid Optional<Course> course) {
		}

		@Valid
		public Course getCourse(Course course) {
			throw new UnsupportedOperationException();
		}

		@Valid
		public List<Course> getCourseList() {
			throw new UnsupportedOperationException();
		}

	}


	private record Course(@NotBlank String title, @Valid Person professor, @Valid List<Course> requiredCourses) {
	}


	@SuppressWarnings("unused")
	private record Person(@Size(min = 1, max = 5) String name) {
	}

}

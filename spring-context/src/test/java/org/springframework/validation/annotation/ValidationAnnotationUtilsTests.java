/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.validation.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidationAnnotationUtils}.
 *
 * @author Sam Brannen
 * @since 7.0.4
 */
class ValidationAnnotationUtilsTests {

	@Nested
	class DetermineValidationHintsTests {

		Method method;

		@RegisterExtension
		BeforeTestExecutionCallback extension = context -> this.method = context.getRequiredTestMethod();

		@Test
		void nonValidatedMethod(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(Test.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).isNull();
		}

		@Test
		@Validated({ GroupA.class, Default.class })
		void springValidated(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(Validated.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).containsExactly(GroupA.class, Default.class);
		}

		@Test
		@MetaValidated
		void springMetaValidated(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(MetaValidated.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).containsExactly(GroupB.class, Default.class);
		}

		@Test  // gh-36274
		@MetaMetaValidated
		void springMetaMetaValidated(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(MetaMetaValidated.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).containsExactly(GroupB.class, Default.class);
		}

		@Test
		@Valid
		void jakartaValid(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(Valid.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).isEmpty();
		}

		@Test
		@ValidPlain
		void plainCustomValidAnnotation(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(ValidPlain.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).isEmpty();
		}

		@Test
		@ValidParameterized
		void parameterizedCustomValidAnnotationWithEmptyGroups(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(ValidParameterized.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).isEmpty();
		}

		@Test
		@ValidParameterized({ GroupA.class, GroupB.class })
		void parameterizedCustomValidAnnotationWithNonEmptyGroups(TestInfo testInfo) {
			var annotation = this.method.getAnnotation(ValidParameterized.class);
			var hints = ValidationAnnotationUtils.determineValidationHints(annotation);

			assertThat(hints).containsExactly(GroupA.class, GroupB.class);
		}


		@Retention(RetentionPolicy.RUNTIME)
		@interface ValidPlain {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@interface ValidParameterized {
			Class<?>[] value() default {};
		}
	}

	@Nested
	class DetermineValidationGroupsTests {

		Method method;

		@RegisterExtension
		BeforeTestExecutionCallback extension = context -> this.method = context.getRequiredTestMethod();

		@Test
		void nonValidatedMethod(TestInfo testInfo) {
			var hints = ValidationAnnotationUtils.determineValidationGroups(this, this.method);

			assertThat(hints).isEmpty();
		}

		@Test
		@Validated({ GroupA.class, Default.class })
		void springValidated(TestInfo testInfo) {
			var hints = ValidationAnnotationUtils.determineValidationGroups(this, this.method);

			assertThat(hints).containsExactly(GroupA.class, Default.class);
		}

		@Test
		@MetaValidated
		void springMetaValidated(TestInfo testInfo) {
			var hints = ValidationAnnotationUtils.determineValidationGroups(this, this.method);

			assertThat(hints).containsExactly(GroupB.class, Default.class);
		}

		@Test
		@MetaMetaValidated
		void springMetaMetaValidated(TestInfo testInfo) {
			var hints = ValidationAnnotationUtils.determineValidationGroups(this, this.method);

			assertThat(hints).containsExactly(GroupB.class, Default.class);
		}

		@Test
		@Valid
		void jakartaValid(TestInfo testInfo) {
			var hints = ValidationAnnotationUtils.determineValidationGroups(this, this.method);

			assertThat(hints).isEmpty();
		}
	}


	interface GroupA {
	}

	interface GroupB {
	}

	@Validated({ GroupB.class, Default.class })
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaValidated {
	}

	@MetaValidated
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaMetaValidated {
	}

}

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

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.EngineTestKitUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.springframework.test.context.junit.EngineTestKitUtils.rootCause;

/**
 * {@link TestBean @TestBean} integration tests for failure scenarios.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see TestBeanIntegrationTests
 */
class FailingTestBeanIntegrationTests {

	@Test
	void testBeanFailingNoFieldNameBean() {
		Class<?> testClass = NoOriginalBeanTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
				cause(
					instanceOf(IllegalStateException.class),
					message("""
						Unable to override bean 'noOriginalBean': there is no bean definition \
						to replace with that name of type java.lang.String"""))));
	}

	@Test
	void testBeanFailingNoExplicitNameBean() {
		Class<?> testClass = BeanDefinitionToOverrideNotPresentTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
				cause(
					instanceOf(IllegalStateException.class),
					message("""
						Unable to override bean 'notPresent': there is no bean definition \
						to replace with that name of type java.lang.String"""))));
	}

	@Test
	void testBeanFailingNoImplicitMethod() {
		Class<?> testClass = ExplicitTestOverrideMethodNotPresentTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
				rootCause(
					instanceOf(IllegalStateException.class),
					message("""
						Failed to find a static test bean factory method in %s with return type \
						java.lang.String whose name matches one of the supported candidates \
						[notPresent]""".formatted(testClass.getName())))));
	}

	@Test
	void testBeanFailingNoExplicitMethod() {
		Class<?> testClass = ImplicitTestOverrideMethodNotPresentTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
				rootCause(
					instanceOf(IllegalStateException.class),
					message("""
						Failed to find a static test bean factory method in %s with return type \
						java.lang.String whose name matches one of the supported candidates \
						[fieldTestOverride]""".formatted(testClass.getName())))));
	}

	@Test
	void testBeanFailingBeanOfWrongType() {
		Class<?> testClass = BeanTypeMismatchTestCase.class;
		EngineTestKitUtils.executeTestsForClass(testClass).assertThatEvents().haveExactly(1,
			finishedWithFailure(
				rootCause(
					instanceOf(IllegalStateException.class),
					message("""
						Unable to override bean 'notString': there is no bean definition to replace \
						with that name of type java.lang.String"""))));
	}


	@SpringJUnitConfig
	static class NoOriginalBeanTestCase {

		@TestBean(name = "noOriginalBean")
		String noOriginalBean;

		@Test
		void test() {
			fail("should fail earlier");
		}

		static String noOriginalBeanTestOverride() {
			return "should be ignored";
		}
	}

	@SpringJUnitConfig
	static class BeanDefinitionToOverrideNotPresentTestCase {

		@TestBean(name = "notPresent")
		String field;

		@Test
		void test() {
			fail("should fail earlier");
		}

		static String notPresentTestOverride() {
			return "should be ignored";
		}
	}

	@SpringJUnitConfig
	static class ExplicitTestOverrideMethodNotPresentTestCase {

		@TestBean(methodName = "notPresent")
		String field;

		@Test
		void test() {
			fail("should fail earlier");
		}
	}

	@SpringJUnitConfig
	static class ImplicitTestOverrideMethodNotPresentTestCase {

		@TestBean // expects fieldTestOverride method
		String field;

		@Test
		void test() {
			fail("should fail earlier");
		}
	}

	@SpringJUnitConfig
	static class BeanTypeMismatchTestCase {

		@TestBean(name = "notString")
		String field;

		@Test
		void test() {
			fail("should fail earlier");
		}

		static String fieldTestOverride() {
			return "should be ignored";
		}

		@Configuration
		static class Config {

			@Bean("notString")
			StringBuilder bean1() {
				return new StringBuilder("not a String");
			}
		}
	}

}

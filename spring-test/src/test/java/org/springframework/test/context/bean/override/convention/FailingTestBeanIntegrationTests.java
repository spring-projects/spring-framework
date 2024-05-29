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
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * {@link TestBean @TestBean} integration tests for failure scenarios.
 *
 * @since 6.2
 * @see TestBeanIntegrationTests
 */
public class FailingTestBeanIntegrationTests {

	@Test
	void testBeanFailingNoFieldNameBean() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(NoOriginalBeanTestCase.class))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.cause()
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Unable to override bean 'noOriginalBean'; " +
								"there is no bean definition to replace with that name of type java.lang.String"));
	}

	@Test
	void testBeanFailingNoExplicitNameBean() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(BeanDefinitionToOverrideNotPresentTestCase.class))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.cause()
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Unable to override bean 'notPresent'; " +
								"there is no bean definition to replace with that name of type java.lang.String"));
	}

	@Test
	void testBeanFailingNoImplicitMethod() {
		Class<?> testClass = ExplicitTestOverrideMethodNotPresentTestCase.class;
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(testClass))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.rootCause().isInstanceOf(IllegalStateException.class)
						.hasMessage("Failed to find a static test bean factory method in %s " +
								"with return type java.lang.String whose name matches one of the " +
								"supported candidates [notPresent]", testClass.getName()));
	}

	@Test
	void testBeanFailingNoExplicitMethod() {
		Class<?> testClass = ImplicitTestOverrideMethodNotPresentTestCase.class;
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(testClass))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.rootCause().isInstanceOf(IllegalStateException.class)
						.hasMessage("Failed to find a static test bean factory method in %s " +
								"with return type java.lang.String whose name matches one of the " +
								"supported candidates [fieldTestOverride]", testClass.getName()));
	}

	@Test
	void testBeanFailingBeanOfWrongType() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(BeanTypeMismatchTestCase.class))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.rootCause().isInstanceOf(IllegalStateException.class)
						.hasMessage("Unable to override bean 'notString'; there is no bean definition to replace with " +
								"that name of type java.lang.String"));
	}

	@Configuration
	static class Config {

		@Bean("field")
		String bean1() {
			return "prod";
		}

		@Bean("nestedField")
		String bean2() {
			return "nestedProd";
		}

		@Bean("methodRenamed1")
		String bean3() {
			return "Prod";
		}

		@Bean("methodRenamed2")
		String bean4() {
			return "NestedProd";
		}
	}

	@SpringJUnitConfig
	@DisabledInAotMode
	static class NoOriginalBeanTestCase {

		@TestBean(name = "noOriginalBean")
		String noOriginalBean;

		@Test
		void ignored() {
			fail("should fail earlier");
		}

		static String noOriginalBeanTestOverride() {
			return "should be ignored";
		}

	}

	@SpringJUnitConfig
	@DisabledInAotMode
	static class BeanDefinitionToOverrideNotPresentTestCase {

		@TestBean(name = "notPresent")
		String field;

		@Test
		void ignored() {
			fail("should fail earlier");
		}

		static String notPresentTestOverride() {
			return "should be ignored";
		}
	}

	@SpringJUnitConfig
	@DisabledInAotMode
	static class ExplicitTestOverrideMethodNotPresentTestCase {

		@TestBean(methodName = "notPresent")
		String field;

		@Test
		void ignored() {
			fail("should fail earlier");
		}
	}

	@SpringJUnitConfig
	@DisabledInAotMode
	static class ImplicitTestOverrideMethodNotPresentTestCase {

		@TestBean //expects fieldTestOverride method
		String field;

		@Test
		void ignored() {
			fail("should fail earlier");
		}
	}

	@SpringJUnitConfig
	@DisabledInAotMode
	static class BeanTypeMismatchTestCase {

		@TestBean(name = "notString")
		String field;

		@Test
		void ignored() {
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

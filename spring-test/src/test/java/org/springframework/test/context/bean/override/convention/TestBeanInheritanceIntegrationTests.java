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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestBeanInheritanceIntegrationTests {

	static AbstractTestBeanIntegrationTestCase.Pojo nestedBeanOverride() {
		return new AbstractTestBeanIntegrationTestCase.FakePojo("in enclosing test class");
	}

	@Nested
	@DisplayName("Concrete inherited test with correct @TestBean setup")
	class ConcreteTestBeanIntegrationTests extends AbstractTestBeanIntegrationTestCase {

		@TestBean(name = "pojo", methodName = "commonBeanOverride")
		Pojo pojo;

		@TestBean(name = "pojo2", methodName = "nestedBeanOverride")
		Pojo pojo2;

		static Pojo someBeanTestOverride() {
			return new FakePojo("someBeanOverride");
		}

		@Test
		void fieldInSupertypeMethodInType(ApplicationContext ctx) {
			assertThat(ctx.getBean("someBean")).as("applicationContext").hasToString("someBeanOverride");
			assertThat(this.someBean.getValue()).as("injection point").isEqualTo("someBeanOverride");
		}

		@Test
		void fieldInTypeMethodInSuperType(ApplicationContext ctx) {
			assertThat(ctx.getBean("pojo")).as("applicationContext").hasToString("in superclass");
			assertThat(this.pojo.getValue()).as("injection point").isEqualTo("in superclass");
		}

		@Test
		void fieldInTypeMethodInEnclosingClass(ApplicationContext ctx) {
			assertThat(ctx.getBean("pojo2")).as("applicationContext").hasToString("in enclosing test class");
			assertThat(this.pojo2.getValue()).as("injection point").isEqualTo("in enclosing test class");
		}

		@Test
		void fieldInSupertypePrioritizeMethodInType(ApplicationContext ctx) {
			assertThat(ctx.getBean("someBean")).as("applicationContext").hasToString("someBeanOverride");
			assertThat(this.someBean.getValue()).as("injection point").isEqualTo("someBeanOverride");
		}
	}


	@Test
	void failsIfFieldInSupertypeButNoMethod() {
		Class<?> clazz = Failing1.class;
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(clazz))//
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.rootCause().isInstanceOf(IllegalStateException.class)
						.hasMessage("""
			Failed to find a static test bean factory method in %s with return type %s \
			whose name matches one of the supported candidates [someBeanTestOverride]""",
			clazz.getName(), AbstractTestBeanIntegrationTestCase.Pojo.class.getName()));
	}

	@Test
	void failsIfMethod1InSupertypeAndMethod2InType() {
		Class<?> clazz = Failing2.class;
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(clazz))
				.execute();

		assertThat(results.allEvents().failed().stream()).hasSize(1).first()
				.satisfies(e -> assertThat(e.getRequiredPayload(TestExecutionResult.class)
						.getThrowable()).get(THROWABLE)
						.rootCause().isInstanceOf(IllegalStateException.class)
						.hasMessage("""
			Found 2 competing static test bean factory methods in %s with return type %s \
			whose name matches one of the supported candidates \
			[thirdBeanTestOverride, anotherBeanTestOverride]""",
			clazz.getName(), AbstractTestBeanIntegrationTestCase.Pojo.class.getName()));
	}

	static class Failing1 extends AbstractTestBeanIntegrationTestCase {

		@Test
		void ignored() {
		}
	}

	static class Failing2 extends AbstractTestBeanIntegrationTestCase {

		static Pojo someBeanTestOverride() {
			return new FakePojo("ignored");
		}

		static Pojo anotherBeanTestOverride() {
			return new FakePojo("sub2");
		}

		@Test
		void ignored2() { }
	}
}

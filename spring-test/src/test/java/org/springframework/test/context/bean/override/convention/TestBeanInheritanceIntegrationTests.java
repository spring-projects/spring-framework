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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.convention.AbstractTestBeanIntegrationTestCase.FakePojo;
import org.springframework.test.context.bean.override.convention.AbstractTestBeanIntegrationTestCase.Pojo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TestBean @TestBean} inheritance integration tests for success scenarios.
 *
 * <p>Tests inheritance within a class hierarchy as well as "inheritance" within
 * an enclosing class hierarchy.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see FailingTestBeanInheritanceIntegrationTests
 */
public class TestBeanInheritanceIntegrationTests {

	static Pojo enclosingClassBeanOverride() {
		return new FakePojo("in enclosing test class");
	}

	@Nested
	@DisplayName("Nested, concrete inherited tests with correct @TestBean setup")
	public class NestedConcreteTestBeanIntegrationTests extends AbstractTestBeanIntegrationTestCase {

		@Autowired
		ApplicationContext ctx;

		@TestBean(name = "pojo", methodName = "commonBeanOverride")
		Pojo pojo;

		@TestBean(name = "pojo2", methodName = "enclosingClassBeanOverride")
		Pojo pojo2;

		static Pojo someBean() {
			return new FakePojo("someBeanOverride");
		}

		// Hides otherBean() defined in AbstractTestBeanIntegrationTestCase.
		static Pojo otherBean() {
			return new FakePojo("otherBean in subclass");
		}

		@Test
		void fieldInSubtypeWithFactoryMethodInSupertype() {
			assertThat(ctx.getBean("pojo")).as("applicationContext").hasToString("in superclass");
			assertThat(this.pojo.getValue()).as("injection point").isEqualTo("in superclass");
		}

		@Test
		void fieldInSupertypeWithFactoryMethodInSubtype() {
			assertThat(ctx.getBean("someBean")).as("applicationContext").hasToString("someBeanOverride");
			assertThat(this.someBean.getValue()).as("injection point").isEqualTo("someBeanOverride");
		}

		@Test
		void fieldInSupertypeWithPrioritizedFactoryMethodInSubtype() {
			assertThat(ctx.getBean("otherBean")).as("applicationContext").hasToString("otherBean in subclass");
			assertThat(super.otherBean.getValue()).as("injection point").isEqualTo("otherBean in subclass");
		}

		@Test
		void fieldInNestedClassWithFactoryMethodInEnclosingClass() {
			assertThat(ctx.getBean("pojo2")).as("applicationContext").hasToString("in enclosing test class");
			assertThat(this.pojo2.getValue()).as("injection point").isEqualTo("in enclosing test class");
		}
	}

}

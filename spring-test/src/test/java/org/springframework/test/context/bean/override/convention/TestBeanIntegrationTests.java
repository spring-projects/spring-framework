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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TestBean @TestBean} integration tests for success scenarios.
 *
 * @since 6.2
 * @see FailingTestBeanIntegrationTests
 */
@SpringJUnitConfig
public class TestBeanIntegrationTests {

	@TestBean(name = "field")
	String field;

	@TestBean(name = "nestedField")
	String nestedField;

	@TestBean(name = "field")
	String renamed1;

	@TestBean(name = "nestedField")
	String renamed2;

	@TestBean(name = "methodRenamed1", methodName = "fieldTestOverride")
	String methodRenamed1;

	@TestBean(name = "methodRenamed2", methodName = "nestedFieldTestOverride")
	String methodRenamed2;

	static String fieldTestOverride() {
		return "fieldOverride";
	}

	static String nestedFieldTestOverride() {
		return "nestedFieldOverride";
	}

	@Test
	void fieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field")).as("applicationContext").isEqualTo("fieldOverride");
		assertThat(this.field).as("injection point").isEqualTo("fieldOverride");
	}

	@Test
	void renamedFieldHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("field")).as("applicationContext").isEqualTo("fieldOverride");
		assertThat(this.renamed1).as("injection point").isEqualTo("fieldOverride");
	}

	@Test
	void fieldWithMethodNameHasOverride(ApplicationContext ctx) {
		assertThat(ctx.getBean("methodRenamed1")).as("applicationContext").isEqualTo("fieldOverride");
		assertThat(this.methodRenamed1).as("injection point").isEqualTo("fieldOverride");
	}


	@Nested
	@DisplayName("With @TestBean on enclosing class")
	class TestBeanNested {

		@Test
		void fieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField")).as("applicationContext").isEqualTo("nestedFieldOverride");
			assertThat(TestBeanIntegrationTests.this.nestedField).isEqualTo("nestedFieldOverride");
		}

		@Test
		void renamedFieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField")).as("applicationContext").isEqualTo("nestedFieldOverride");
			assertThat(TestBeanIntegrationTests.this.renamed2).isEqualTo("nestedFieldOverride");
		}

		@Test
		void fieldWithMethodNameHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("methodRenamed2")).as("applicationContext").isEqualTo("nestedFieldOverride");
			assertThat(TestBeanIntegrationTests.this.methodRenamed2).isEqualTo("nestedFieldOverride");
		}
	}

	@Nested
	@DisplayName("With factory method on enclosing class")
	class TestBeanNested2 {

		@TestBean(methodName = "nestedFieldTestOverride", name = "nestedField")
		String nestedField2;

		@Test
		void fieldHasOverride(ApplicationContext ctx) {
			assertThat(ctx.getBean("nestedField")).as("applicationContext").isEqualTo("nestedFieldOverride");
			assertThat(this.nestedField2).isEqualTo("nestedFieldOverride");
		}
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

}

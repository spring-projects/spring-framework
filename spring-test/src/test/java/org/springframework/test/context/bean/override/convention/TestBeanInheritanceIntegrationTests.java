/*
 * Copyright 2002-2025 the original author or authors.
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestBean} that use inheritance.
 *
 * <p>Tests inheritance within a class hierarchy as well as "inheritance" within
 * an enclosing class hierarchy.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
public class TestBeanInheritanceIntegrationTests {

	@TestBean
	Pojo puzzleBean;

	static Pojo puzzleBean() {
		return new FakePojo("puzzle in enclosing class");
	}

	static Pojo enclosingClassBean() {
		return new FakePojo("in enclosing test class");
	}

	abstract static class AbstractTestCase {

		@TestBean
		Pojo someBean;

		@TestBean("otherBean")
		Pojo otherBean;

		@TestBean("thirdBean")
		Pojo anotherBean;

		@TestBean
		Pojo enigmaBean;

		static Pojo otherBean() {
			return new FakePojo("other in superclass");
		}

		static Pojo thirdBean() {
			return new FakePojo("third in superclass");
		}

		static Pojo enigmaBean() {
			return new FakePojo("enigma in superclass");
		}

		static Pojo commonBean() {
			return new FakePojo("common in superclass");
		}
	}

	@Nested
	@DisplayName("Nested, concrete inherited tests with correct @TestBean setup")
	class NestedTests extends AbstractTestCase {

		@Autowired
		ApplicationContext ctx;

		@TestBean(methodName = "commonBean")
		Pojo pojo;

		@TestBean(name = "pojo2", methodName = "enclosingClassBean")
		Pojo pojo2;

		@TestBean(methodName = "localEnigmaBean")
		Pojo enigmaBean;

		@TestBean
		Pojo puzzleBean;


		static Pojo puzzleBean() {
			return new FakePojo("puzzle in nested class");
		}

		static Pojo localEnigmaBean() {
			return new FakePojo("enigma in subclass");
		}

		static Pojo someBean() {
			return new FakePojo("someBeanOverride");
		}

		// "Overrides" otherBean() defined in AbstractTestBeanIntegrationTestCase.
		static Pojo otherBean() {
			return new FakePojo("other in subclass");
		}

		@Test
		void fieldInSuperclassWithFactoryMethodInSuperclass() {
			assertThat(ctx.getBean("thirdBean")).as("applicationContext").hasToString("third in superclass");
			assertThat(super.anotherBean.value()).as("injection point").isEqualTo("third in superclass");
		}

		@Test
		void fieldInSuperclassWithFactoryMethodInSubclass() {
			assertThat(ctx.getBean("someBean")).as("applicationContext").hasToString("someBeanOverride");
			assertThat(super.someBean.value()).as("injection point").isEqualTo("someBeanOverride");
		}

		@Test
		void fieldInSuperclassWithFactoryMethodInSupeclassAndInSubclass() {
			assertThat(ctx.getBean("otherBean")).as("applicationContext").hasToString("other in subclass");
			assertThat(super.otherBean.value()).as("injection point").isEqualTo("other in subclass");
		}

		@Test
		void fieldInSubclassWithFactoryMethodInSuperclass() {
			assertThat(ctx.getBean("pojo")).as("applicationContext").hasToString("common in superclass");
			assertThat(this.pojo.value()).as("injection point").isEqualTo("common in superclass");
		}

		@Test
		void fieldInNestedClassWithFactoryMethodInEnclosingClass() {
			assertThat(ctx.getBean("pojo2")).as("applicationContext").hasToString("in enclosing test class");
			assertThat(this.pojo2.value()).as("injection point").isEqualTo("in enclosing test class");
		}

		@Test  // gh-34194
		void testBeanInSubclassOverridesTestBeanInSuperclass() {
			assertThat(ctx.getBean("enigmaBean")).as("applicationContext").hasToString("enigma in subclass");
			assertThat(this.enigmaBean.value()).as("injection point").isEqualTo("enigma in subclass");
		}

		@Test  // gh-34194
		void testBeanInNestedClassOverridesTestBeanInEnclosingClass() {
			assertThat(ctx.getBean("puzzleBean")).as("applicationContext").hasToString("puzzle in nested class");
			assertThat(this.puzzleBean.value()).as("injection point").isEqualTo("puzzle in nested class");
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		Pojo someBean() {
			return new ProdPojo();
		}

		@Bean
		Pojo otherBean() {
			return new ProdPojo();
		}

		@Bean
		Pojo thirdBean() {
			return new ProdPojo();
		}

		@Bean
		Pojo enigmaBean() {
			return new ProdPojo();
		}

		@Bean
		Pojo puzzleBean() {
			return new ProdPojo();
		}

		@Bean
		Pojo pojo() {
			return new ProdPojo();
		}

		@Bean
		Pojo pojo2() {
			return new ProdPojo();
		}
	}

	interface Pojo {

		default String value() {
			return "prod";
		}
	}

	static class ProdPojo implements Pojo {

		@Override
		public String toString() {
			return value();
		}
	}

	record FakePojo(String value) implements Pojo {

		@Override
		public String toString() {
			return value();
		}
	}

}

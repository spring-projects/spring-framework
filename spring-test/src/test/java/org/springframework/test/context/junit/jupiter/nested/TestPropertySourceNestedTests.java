/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.TestPropertySourceNestedTests.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestPropertySource @TestPropertySource} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@SpringJUnitConfig(Config.class)
@TestPropertySource(properties = "p1 = v1")
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestPropertySourceNestedTests {

	@Autowired
	Environment env1;


	@Test
	void propertiesInEnvironment() {
		assertThat(env1.getProperty("p1")).isEqualTo("v1");
	}


	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		@Autowired
		Environment env2;


		@Test
		void propertiesInEnvironment() {
			assertThat(env1.getProperty("p1")).isEqualTo("v1");
			assertThat(env2.getProperty("p1")).isEqualTo("v1");
			assertThat(env1).isSameAs(env2);
		}
	}

	@Nested
	@SpringJUnitConfig(Config.class)
	@TestPropertySource(properties = "p2 = v2")
	class ConfigOverriddenByDefaultTests {

		@Autowired
		Environment env2;


		@Test
		void propertiesInEnvironment() {
			assertThat(env1.getProperty("p1")).isEqualTo("v1");
			assertThat(env1).isNotSameAs(env2);
			assertThat(env2.getProperty("p1")).isNull();
			assertThat(env2.getProperty("p2")).isEqualTo("v2");
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@TestPropertySource(properties = "p2a = v2a")
	@TestPropertySource(properties = "p2b = v2b")
	class InheritedAndExtendedConfigTests {

		@Autowired
		Environment env2;


		@Test
		void propertiesInEnvironment() {
			assertThat(env1.getProperty("p1")).isEqualTo("v1");
			assertThat(env1).isNotSameAs(env2);
			assertThat(env2.getProperty("p1")).isEqualTo("v1");
			assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
			assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(Config.class)
		@TestPropertySource(properties = "p3 = v3")
		class L3WithOverriddenConfigTests {

			@Autowired
			Environment env3;


			@Test
			void propertiesInEnvironment() {
				assertThat(env1.getProperty("p1")).isEqualTo("v1");
				assertThat(env1).isNotSameAs(env2);
				assertThat(env2.getProperty("p1")).isEqualTo("v1");
				assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
				assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
				assertThat(env2).isNotSameAs(env3);
				assertThat(env3.getProperty("p1")).isNull();
				assertThat(env3.getProperty("p2")).isNull();
				assertThat(env3.getProperty("p3")).isEqualTo("v3");
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			@TestPropertySource(properties = {"p3 = v34", "p4 = v4"}, inheritProperties = false)
			class L4WithInheritedConfigButOverriddenTestPropertiesTests {

				@Autowired
				Environment env4;


				@Test
				void propertiesInEnvironment() {
					assertThat(env1.getProperty("p1")).isEqualTo("v1");
					assertThat(env1).isNotSameAs(env2);
					assertThat(env2.getProperty("p1")).isEqualTo("v1");
					assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
					assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
					assertThat(env2).isNotSameAs(env3);
					assertThat(env3.getProperty("p1")).isNull();
					assertThat(env3.getProperty("p2")).isNull();
					assertThat(env3.getProperty("p3")).isEqualTo("v3");
					assertThat(env3).isNotSameAs(env4);
					assertThat(env4.getProperty("p1")).isNull();
					assertThat(env4.getProperty("p2")).isNull();
					assertThat(env4.getProperty("p3")).isEqualTo("v34");
					assertThat(env4.getProperty("p4")).isEqualTo("v4");
				}

				@Nested
				class L5WithInheritedConfigAndTestInterfaceTests implements TestInterface {

					@Autowired
					Environment env5;


					@Test
					void propertiesInEnvironment() {
						assertThat(env4).isNotSameAs(env5);
						assertThat(env5.getProperty("foo")).isEqualTo("bar");
						assertThat(env5.getProperty("enigma")).isEqualTo("42");
						assertThat(env5.getProperty("p1")).isNull();
						assertThat(env5.getProperty("p2")).isNull();
						assertThat(env5.getProperty("p3")).isEqualTo("v34");
						assertThat(env5.getProperty("p4")).isEqualTo("v4");
					}
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

	@TestPropertySource(properties = { "foo = bar", "enigma: 42" })
	interface TestInterface {
	}

}

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

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.ActiveProfilesNestedTests.Config1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ActiveProfiles @ActiveProfiles} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@SpringJUnitConfig(Config1.class)
@ActiveProfiles("1")
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ActiveProfilesNestedTests {

	@Autowired
	List<String> strings;


	@Test
	void test() {
		assertThat(this.strings).containsExactlyInAnyOrder("X", "A1");
	}


	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		@Autowired
		List<String> localStrings;


		@Test
		void test() {
			assertThat(strings).containsExactlyInAnyOrder("X", "A1");
			assertThat(this.localStrings).containsExactlyInAnyOrder("X", "A1");
		}
	}

	@Nested
	@SpringJUnitConfig(Config2.class)
	@ActiveProfiles("2")
	class ConfigOverriddenByDefaultTests {

		@Autowired
		List<String> localStrings;


		@Test
		void test() {
			assertThat(strings).containsExactlyInAnyOrder("X", "A1");
			assertThat(this.localStrings).containsExactlyInAnyOrder("Y", "A2");
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@ContextConfiguration(classes = Config2.class)
	@ActiveProfiles("2")
	class InheritedAndExtendedConfigTests {

		@Autowired
		List<String> localStrings;


		@Test
		void test() {
			assertThat(strings).containsExactlyInAnyOrder("X", "A1");
			assertThat(this.localStrings).containsExactlyInAnyOrder("X", "A1", "Y", "A2");
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig({ Config1.class, Config2.class, Config3.class })
		@ActiveProfiles("3")
		class DoubleNestedWithOverriddenConfigTests {

			@Autowired
			List<String> localStrings;


			@Test
			void test() {
				assertThat(strings).containsExactlyInAnyOrder("X", "A1");
				assertThat(this.localStrings).containsExactlyInAnyOrder("X", "Y", "Z", "A3");
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			@ActiveProfiles(profiles = "2", inheritProfiles = false)
			class TripleNestedWithInheritedConfigButOverriddenProfilesTests {

				@Autowired
				List<String> localStrings;


				@Test
				void test() {
					assertThat(strings).containsExactlyInAnyOrder("X", "A1");
					assertThat(this.localStrings).containsExactlyInAnyOrder("X", "Y", "Z", "A2");
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Autowired
				List<String> localStrings;


				@Test
				void test() {
					assertThat(strings).containsExactlyInAnyOrder("X", "A1");
					assertThat(this.localStrings).containsExactlyInAnyOrder("X", "Y", "Z", "A2", "A3");
				}
			}
		}

	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config1 {

		@Bean
		String x() {
			return "X";
		}

		@Bean
		@Profile("1")
		String a1() {
			return "A1";
		}
	}

	@Configuration
	static class Config2 {

		@Bean
		String y() {
			return "Y";
		}

		@Bean
		@Profile("2")
		String a2() {
			return "A2";
		}
	}

	@Configuration
	static class Config3 {

		@Bean
		String z() {
			return "Z";
		}

		@Bean
		@Profile("3")
		String a3() {
			return "A3";
		}
	}

	@ActiveProfiles("2")
	interface TestInterface {
	}

}

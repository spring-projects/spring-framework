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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.TestConstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.springframework.test.context.TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME;

/**
 * Integration tests for {@link TestConstructor @TestConstructor} support.
 *
 * @author Sam Brannen
 * @since 5.3
 */
class TestConstructorIntegrationTests {

	@BeforeEach
	@AfterEach
	void clearSpringProperty() {
		setSpringProperty(null);
	}

	@Test
	void autowireModeNotSetToAll() {
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(AutomaticallyAutowiredTestCase.class))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(1).succeeded(0).failed(1))
			.assertThatEvents().haveExactly(1, event(test("test"),
				finishedWithFailure(
					instanceOf(ParameterResolutionException.class),
					message(msg -> msg.matches(".+for parameter \\[java\\.lang\\.String .+\\] in constructor.+")))));
	}

	@Test
	void autowireModeSetToAllViaSpringProperties() {
		setSpringProperty("all");

		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(AutomaticallyAutowiredTestCase.class))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}

	@Test
	void autowireModeSetToAllViaJUnitPlatformConfigurationParameter() {
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(AutomaticallyAutowiredTestCase.class))
			.configurationParameter(TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME, "all")
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
	}


	private void setSpringProperty(String flag) {
		SpringProperties.setProperty(TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME, flag);
	}


	@SpringJUnitConfig
	@FailingTestCase
	static class AutomaticallyAutowiredTestCase {

		private final String foo;


		AutomaticallyAutowiredTestCase(String foo) {
			this.foo = foo;
		}

		@Test
		void test() {
			assertThat(foo).isEqualTo("bar");
		}


		@Configuration
		static class Config {

			@Bean
			String foo() {
				return "bar";
			}
		}
	}

}

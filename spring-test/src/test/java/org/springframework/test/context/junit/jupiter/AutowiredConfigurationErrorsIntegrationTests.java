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

import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.EventConditions;
import org.junit.platform.testkit.engine.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.allOf;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Integration tests for {@link Autowired @Autowired} configuration errors in
 * JUnit Jupiter test classes.
 *
 * @author Sam Brannen
 * @since 5.3.2
 */
class AutowiredConfigurationErrorsIntegrationTests {

	private static final String DISPLAY_NAME = "TEST";


	@ParameterizedTest
	@ValueSource(classes = {
		StaticAutowiredBeforeAllMethod.class,
		StaticAutowiredAfterAllMethod.class,
		AutowiredBeforeEachMethod.class,
		AutowiredAfterEachMethod.class,
		AutowiredTestMethod.class,
		AutowiredRepeatedTestMethod.class,
		AutowiredParameterizedTestMethod.class
	})
	void autowiredTestMethodsTestTemplateMethodsAndLifecyleMethods(Class<?> testClass) {
		testEventsFor(testClass)
			.assertStatistics(stats -> stats.started(1).succeeded(0).failed(1))
			.assertThatEvents().haveExactly(1,
				event(testWithDisplayName(DISPLAY_NAME),
				finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message(msg -> msg.matches(".+must not be annotated with @Autowired.+")))));
	}

	/**
	 * A non-autowired test method should fail the same as an autowired test
	 * method in the same class, since Spring still should not autowire the
	 * autowired test method as a "configuration method" when JUnit attempts to
	 * execute the non-autowired test method.
	 */
	@Test
	void autowiredAndNonAutowiredTestMethods() {
		testEventsFor(AutowiredAndNonAutowiredTestMethods.class)
			.assertStatistics(stats -> stats.started(2).succeeded(0).failed(2))
			.assertThatEvents()
				.haveExactly(1,
					event(testWithDisplayName("autowired(TestInfo)"),
					finishedWithFailure(
						instanceOf(IllegalStateException.class),
						message(msg -> msg.matches(".+must not be annotated with @Autowired.+")))))
				.haveExactly(1,
					event(testWithDisplayName("nonAutowired(TestInfo)"),
					finishedWithFailure(
						instanceOf(IllegalStateException.class),
						message(msg -> msg.matches(".+must not be annotated with @Autowired.+")))));
	}


	@ParameterizedTest
	@ValueSource(classes = {
		NonStaticAutowiredBeforeAllMethod.class,
		NonStaticAutowiredAfterAllMethod.class
	})
	void autowiredNonStaticClassLevelLifecyleMethods(Class<?> testClass) {
		containerEventsFor(testClass)
			.assertStatistics(stats -> stats.started(2).succeeded(1).failed(1))
			.assertThatEvents().haveExactly(1,
				event(container(),
				finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message(msg -> msg.matches(".+must not be annotated with @Autowired.+")))));
	}

	@Test
	void autowiredTestFactoryMethod() {
		containerEventsFor(AutowiredTestFactoryMethod.class)
			.assertStatistics(stats -> stats.started(3).succeeded(2).failed(1))
			.assertThatEvents().haveExactly(1,
				event(container(),
				finishedWithFailure(
					instanceOf(IllegalStateException.class),
					message(msg -> msg.matches(".+must not be annotated with @Autowired.+")))));
	}

	private Events testEventsFor(Class<?> testClass) {
		return EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(testClass))
			.execute()
			.testEvents();
	}

	private Events containerEventsFor(Class<?> testClass) {
		return EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(testClass))
			.execute()
			.containerEvents();
	}

	private static Condition<Event> testWithDisplayName(String displayName) {
		return allOf(EventConditions.test(), EventConditions.displayName(displayName));
	}


	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class StaticAutowiredBeforeAllMethod {

		@Autowired
		@BeforeAll
		static void beforeAll(TestInfo testInfo) {
		}

		@Test
		@DisplayName(DISPLAY_NAME)
		void test() {
		}
	}

	@SpringJUnitConfig(Config.class)
	@TestInstance(PER_CLASS)
	@FailingTestCase
	static class NonStaticAutowiredBeforeAllMethod {

		@Autowired
		@BeforeAll
		void beforeAll(TestInfo testInfo) {
		}

		@Test
		@DisplayName(DISPLAY_NAME)
		void test() {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class StaticAutowiredAfterAllMethod {

		@Test
		@DisplayName(DISPLAY_NAME)
		void test() {
		}

		@AfterAll
		@Autowired
		static void afterAll(TestInfo testInfo) {
		}
	}

	@SpringJUnitConfig(Config.class)
	@TestInstance(PER_CLASS)
	@FailingTestCase
	static class NonStaticAutowiredAfterAllMethod {

		@Test
		@DisplayName(DISPLAY_NAME)
		void test() {
		}

		@AfterAll
		@Autowired
		void afterAll(TestInfo testInfo) {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredBeforeEachMethod {

		@Autowired
		@BeforeEach
		void beforeEach(TestInfo testInfo) {
		}

		@Test
		@DisplayName(DISPLAY_NAME)
		void test() {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredAfterEachMethod {

		@Test
		@DisplayName(DISPLAY_NAME)
		void test() {
		}

		@Autowired
		@AfterEach
		void afterEach(TestInfo testInfo) {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredTestMethod {

		@Autowired
		@Test
		@DisplayName(DISPLAY_NAME)
		void test(TestInfo testInfo) {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredAndNonAutowiredTestMethods {

		@Autowired
		@Test
		void autowired(TestInfo testInfo) {
		}

		@Test
		void nonAutowired(TestInfo testInfo) {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredRepeatedTestMethod {

		@Autowired
		@RepeatedTest(value = 1, name = DISPLAY_NAME)
		void test(TestInfo testInfo) {
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredTestFactoryMethod {

		@Autowired
		@TestFactory
		Stream<DynamicTest> testFactory(TestInfo testInfo) {
			return Stream.of(dynamicTest("dynamicTest", () -> {}));
		}
	}

	@SpringJUnitConfig(Config.class)
	@FailingTestCase
	static class AutowiredParameterizedTestMethod {

		@Autowired
		@ParameterizedTest(name = DISPLAY_NAME)
		@ValueSource(strings = "ignored")
		void test(TestInfo testInfo) {
		}
	}

	@Configuration
	static class Config {
	}

}


/*
 * Copyright 2002-present the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Integration tests which verify that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@code @BeforeTransaction} and {@code @AfterTransaction} methods can fail
 * tests run via the {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * <p>See: <a href="https://jira.spring.io/browse/SPR-3960" target="_blank">SPR-3960</a>
 * and <a href="https://jira.spring.io/browse/SPR-4365" target="_blank">SPR-4365</a>.
 *
 * <p>Indirectly, this class also verifies that all {@code TestExecutionListener}
 * lifecycle callbacks are called.
 *
 * @author Sam Brannen
 * @since 5.0
 */
class FailingBeforeAndAfterMethodsSpringExtensionTests {

	@ParameterizedTest
	@ValueSource(classes = {
		AlwaysFailingBeforeTestClassTestCase.class,
		AlwaysFailingAfterTestClassTestCase.class,
		AlwaysFailingPrepareTestInstanceTestCase.class,
		AlwaysFailingBeforeTestMethodTestCase.class,
		AlwaysFailingBeforeTestExecutionTestCase.class,
		AlwaysFailingAfterTestExecutionTestCase.class,
		AlwaysFailingAfterTestMethodTestCase.class,
		FailingBeforeTransactionTestCase.class,
		FailingAfterTransactionTestCase.class
	})
	void failingBeforeAndAfterCallbacks(Class<?> testClass) {
		Events events = EngineTestKit.engine("junit-jupiter")
			.selectors(selectClass(testClass))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats
				.skipped(0)
				.aborted(0)
				.started(getExpectedStartedCount(testClass))
				.succeeded(getExpectedSucceededCount(testClass))
				.failed(getExpectedFailedCount(testClass)));

		// Ensure it was an AssertionError that failed the test and not
		// something else like an error in the @Configuration class, etc.
		if (getExpectedFailedCount(testClass) > 0) {
			events.assertThatEvents().haveExactly(1,
				event(test("testNothing"),
					finishedWithFailure(
						instanceOf(AssertionError.class),
						message(msg -> msg.contains("always failing")))));
		}
	}

	private int getExpectedStartedCount(Class<?> testClass) {
		return (testClass == AlwaysFailingBeforeTestClassTestCase.class ? 0 : 1);
	}

	private int getExpectedSucceededCount(Class<?> testClass) {
		return (testClass == AlwaysFailingAfterTestClassTestCase.class ? 1 : 0);
	}

	private int getExpectedFailedCount(Class<?> testClass) {
		if (testClass == AlwaysFailingBeforeTestClassTestCase.class ||
				testClass == AlwaysFailingAfterTestClassTestCase.class) {
			return 0;
		}
		return 1;
	}


	// -------------------------------------------------------------------

	private static class AlwaysFailingBeforeTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			fail("always failing beforeTestClass()");
		}
	}

	private static class AlwaysFailingAfterTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("always failing afterTestClass()");
		}
	}

	private static class AlwaysFailingPrepareTestInstanceTestExecutionListener implements TestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) {
			fail("always failing prepareTestInstance()");
		}
	}

	private static class AlwaysFailingBeforeTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			fail("always failing beforeTestMethod()");
		}
	}

	private static class AlwaysFailingBeforeTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestExecution(TestContext testContext) {
			fail("always failing beforeTestExecution()");
		}
	}

	private static class AlwaysFailingAfterTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("always failing afterTestMethod()");
		}
	}

	private static class AlwaysFailingAfterTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("always failing afterTestExecution()");
		}
	}

	@FailingTestCase
	@ExtendWith(SpringExtension.class)
	private abstract static class BaseTestCase {

		@Test
		void testNothing() {
		}
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestExecutionTestExecutionListener.class)
	static class AlwaysFailingBeforeTestExecutionTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestExecutionTestExecutionListener.class)
	static class AlwaysFailingAfterTestExecutionTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
	}

	@FailingTestCase
	@SpringJUnitConfig(DatabaseConfig.class)
	@Transactional
	static class FailingBeforeTransactionTestCase {

		@Test
		void testNothing() {
		}

		@BeforeTransaction
		void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@FailingTestCase
	@SpringJUnitConfig(DatabaseConfig.class)
	@Transactional
	static class FailingAfterTransactionTestCase {

		@Test
		void testNothing() {
		}

		@AfterTransaction
		void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

	// Must not be private.
	@Configuration
	static class DatabaseConfig {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}
	}

}

/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4;

import java.lang.reflect.Constructor;

import org.junit.experimental.ParallelComputer;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import org.springframework.beans.BeanUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Collection of utilities for testing the execution of JUnit 4 based tests.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see TrackingRunListener
 */
public class JUnitTestingUtils {

	/**
	 * Run the tests in the supplied {@code testClass}, using the {@link Runner}
	 * configured via {@link RunWith @RunWith} or the default JUnit runner, and
	 * assert the expectations of the test execution.
	 *
	 * @param testClass the test class to run with JUnit
	 * @param expectedStartedCount the expected number of tests that started
	 * @param expectedFailedCount the expected number of tests that failed
	 * @param expectedFinishedCount the expected number of tests that finished
	 * @param expectedIgnoredCount the expected number of tests that were ignored
	 * @param expectedAssumptionFailedCount the expected number of tests that
	 * resulted in a failed assumption
	 */
	public static void runTestsAndAssertCounters(Class<?> testClass, int expectedStartedCount, int expectedFailedCount,
			int expectedFinishedCount, int expectedIgnoredCount, int expectedAssumptionFailedCount) throws Exception {

		runTestsAndAssertCounters(null, testClass, expectedStartedCount, expectedFailedCount, expectedFinishedCount,
			expectedIgnoredCount, expectedAssumptionFailedCount);
	}

	/**
	 * Run the tests in the supplied {@code testClass}, using the specified
	 * {@link Runner}, and assert the expectations of the test execution.
	 *
	 * <p>If the specified {@code runnerClass} is {@code null}, the tests
	 * will be run with the runner that the test class is configured with
	 * (i.e., via {@link RunWith @RunWith}) or the default JUnit runner.
	 *
	 * @param runnerClass the explicit runner class to use or {@code null}
	 * if the default JUnit runner should be used
	 * @param testClass the test class to run with JUnit
	 * @param expectedStartedCount the expected number of tests that started
	 * @param expectedFailedCount the expected number of tests that failed
	 * @param expectedFinishedCount the expected number of tests that finished
	 * @param expectedIgnoredCount the expected number of tests that were ignored
	 * @param expectedAssumptionFailedCount the expected number of tests that
	 * resulted in a failed assumption
	 */
	public static void runTestsAndAssertCounters(Class<? extends Runner> runnerClass, Class<?> testClass,
			int expectedStartedCount, int expectedFailedCount, int expectedFinishedCount, int expectedIgnoredCount,
			int expectedAssumptionFailedCount) throws Exception {

		TrackingRunListener listener = new TrackingRunListener();

		if (runnerClass != null) {
			Constructor<?> constructor = runnerClass.getConstructor(Class.class);
			Runner runner = (Runner) BeanUtils.instantiateClass(constructor, testClass);
			RunNotifier notifier = new RunNotifier();
			notifier.addListener(listener);
			runner.run(notifier);
		}
		else {
			JUnitCore junit = new JUnitCore();
			junit.addListener(listener);
			junit.run(testClass);
		}

		assertSoftly(softly -> {
			softly.assertThat(listener.getTestStartedCount()).as("tests started for [%s]", testClass)
				.isEqualTo(expectedStartedCount);
			softly.assertThat(listener.getTestFailureCount()).as("tests failed for [%s]", testClass)
				.isEqualTo(expectedFailedCount);
			softly.assertThat(listener.getTestFinishedCount()).as("tests finished for [%s]", testClass)
				.isEqualTo(expectedFinishedCount);
			softly.assertThat(listener.getTestIgnoredCount()).as("tests ignored for [%s]", testClass)
				.isEqualTo(expectedIgnoredCount);
			softly.assertThat(listener.getTestAssumptionFailureCount()).as("failed assumptions for [%s]", testClass)
				.isEqualTo(expectedAssumptionFailedCount);
		});
	}

	/**
	 * Run all tests in the supplied test classes according to the policies of
	 * the supplied {@link Computer}, using the {@link Runner} configured via
	 * {@link RunWith @RunWith} or the default JUnit runner, and assert the
	 * expectations of the test execution.
	 *
	 * <p>To have all tests executed in parallel, supply {@link ParallelComputer#methods()}
	 * as the {@code Computer}. To have all tests executed serially, supply
	 * {@link Computer#serial()} as the {@code Computer}.
	 *
	 * @param computer the JUnit {@code Computer} to use
	 * @param expectedStartedCount the expected number of tests that started
	 * @param expectedFailedCount the expected number of tests that failed
	 * @param expectedFinishedCount the expected number of tests that finished
	 * @param expectedIgnoredCount the expected number of tests that were ignored
	 * @param expectedAssumptionFailedCount the expected number of tests that
	 * resulted in a failed assumption
	 * @param testClasses one or more test classes to run
	 */
	public static void runTestsAndAssertCounters(Computer computer, int expectedStartedCount, int expectedFailedCount,
			int expectedFinishedCount, int expectedIgnoredCount, int expectedAssumptionFailedCount,
			Class<?>... testClasses) throws Exception {

		JUnitCore junit = new JUnitCore();
		TrackingRunListener listener = new TrackingRunListener();
		junit.addListener(listener);
		junit.run(computer, testClasses);

		assertSoftly(softly -> {
			softly.assertThat(listener.getTestStartedCount()).as("tests started]").isEqualTo(expectedStartedCount);
			softly.assertThat(listener.getTestFailureCount()).as("tests failed]").isEqualTo(expectedFailedCount);
			softly.assertThat(listener.getTestFinishedCount()).as("tests finished]").isEqualTo(expectedFinishedCount);
			softly.assertThat(listener.getTestIgnoredCount()).as("tests ignored]").isEqualTo(expectedIgnoredCount);
			softly.assertThat(listener.getTestAssumptionFailureCount()).as("failed assumptions]").isEqualTo(expectedAssumptionFailedCount);
		});
	}

}

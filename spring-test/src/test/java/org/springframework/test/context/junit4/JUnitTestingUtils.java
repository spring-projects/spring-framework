/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import java.lang.reflect.Constructor;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import org.springframework.beans.BeanUtils;

import static org.junit.Assert.*;

/**
 * Collection of utilities for testing the execution of JUnit tests.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see TrackingRunListener
 */
public class JUnitTestingUtils {

	/**
	 * Run the tests in the supplied {@code testClass}, using the {@link Runner}
	 * it is configured with (i.e., via {@link RunWith @RunWith}) or the default
	 * JUnit runner, and assert the expectations of the test execution.
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
	 * if the implicit runner should be used
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

		assertEquals("tests started for [" + testClass + "]:", expectedStartedCount, listener.getTestStartedCount());
		assertEquals("tests failed for [" + testClass + "]:", expectedFailedCount, listener.getTestFailureCount());
		assertEquals("tests finished for [" + testClass + "]:", expectedFinishedCount, listener.getTestFinishedCount());
		assertEquals("tests ignored for [" + testClass + "]:", expectedIgnoredCount, listener.getTestIgnoredCount());
		assertEquals("failed assumptions for [" + testClass + "]:", expectedAssumptionFailedCount, listener.getTestAssumptionFailureCount());
	}

}

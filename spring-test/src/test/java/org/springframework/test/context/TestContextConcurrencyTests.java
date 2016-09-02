/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import org.junit.Test;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Integration tests that verify proper concurrency support between a
 * {@link TestContextManager} and the {@link TestContext} it manages
 * when a registered {@link TestExecutionListener} updates the mutable
 * state and attributes of the context from concurrently executing threads.
 *
 * <p>In other words, these tests verify that mutated state and attributes
 * are only be visible to the thread in which the mutation occurred.
 *
 * @author Sam Brannen
 * @since 5.0
 */
public class TestContextConcurrencyTests {

	private static Set<String> expectedMethods = stream(TestCase.class.getDeclaredMethods()).map(
		Method::getName).collect(toCollection(TreeSet::new));

	private static final Set<String> actualMethods = Collections.synchronizedSet(new TreeSet<>());

	private static final TestCase testInstance = new TestCase();


	@Test
	public void invokeTestContextManagerFromConcurrentThreads() {
		TestContextManager tcm = new TestContextManager(TestCase.class);

		// Run the actual test several times in order to increase the chance of threads
		// stepping on each others' toes by overwriting the same mutable state in the
		// TestContext.
		IntStream.range(1, 20).forEach(i -> {
			actualMethods.clear();
			// Execute TestExecutionListener in parallel, thereby simulating parallel
			// test method execution.
			stream(TestCase.class.getDeclaredMethods()).parallel().forEach(testMethod -> {
				try {
					tcm.beforeTestClass();
					tcm.beforeTestMethod(testInstance, testMethod);
					// no need to invoke the actual test method
					tcm.afterTestMethod(testInstance, testMethod, null);
					tcm.afterTestClass();
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
			assertThat(actualMethods, equalTo(expectedMethods));
		});
		assertEquals(0, tcm.getTestContext().attributeNames().length);
	}


	@TestExecutionListeners(TrackingListener.class)
	@SuppressWarnings("unused")
	private static class TestCase {

		void test_001() {
		}

		void test_002() {
		}

		void test_003() {
		}

		void test_004() {
		}

		void test_005() {
		}

		void test_006() {
		}

		void test_007() {
		}

		void test_008() {
		}

		void test_009() {
		}

		void test_010() {
		}
	}

	private static class TrackingListener implements TestExecutionListener {

		private ThreadLocal<String> methodName = new ThreadLocal<>();


		@Override
		public void beforeTestMethod(TestContext testContext) throws Exception {
			String name = testContext.getTestMethod().getName();
			actualMethods.add(name);
			testContext.setAttribute("method", name);
			this.methodName.set(name);
		}

		@Override
		public void afterTestMethod(TestContext testContext) throws Exception {
			assertEquals(this.methodName.get(), testContext.getAttribute("method"));
		}

	}

}

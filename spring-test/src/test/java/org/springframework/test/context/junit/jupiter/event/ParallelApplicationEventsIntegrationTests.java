/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.ApplicationEventsHolder;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.event.TestContextEvent;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests that verify parallel execution support for {@link ApplicationEvents}
 * in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 5.3.3
 */
class ParallelApplicationEventsIntegrationTests {

	private static final Set<String> payloads = ConcurrentHashMap.newKeySet();

	@Test
	void rejectTestsInParallelWithInstancePerClassAndRecordApplicationEvents() {
		Class<?> testClass = TestInstancePerClassTestCase.class;

		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(testClass))//
				.configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
				.configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")//
				.configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
				.execute();

		// extract the messages from failed TextExecutionResults
		assertThat(results.containerEvents().failed()//
					.map(e -> e.getRequiredPayload(TestExecutionResult.class).getThrowable().get().getMessage()))//
				.singleElement(InstanceOfAssertFactories.STRING)
				.isEqualTo("""
					Test classes or @Nested test classes that @RecordApplicationEvents must not be run \
					in parallel with the @TestInstance(PER_CLASS) lifecycle mode. Configure either \
					@Execution(SAME_THREAD) or @TestInstance(PER_METHOD) semantics, or disable parallel \
					execution altogether. Note that when recording events in parallel, one might see events \
					published by other tests since the application context may be shared.""");
	}

	@Test
	void executeTestsInParallelWithInstancePerMethod() {
		Class<?> testClass = TestInstancePerMethodTestCase.class;
		Events testEvents = EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(testClass))//
				.configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
				.configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
				.execute()//
				.testEvents();
		// list failed events in case of test errors to get a sense of which tests failed
		Events failedTests = testEvents.failed();
		if (failedTests.count() > 0) {
			failedTests.debug();
		}
		testEvents.assertStatistics(stats -> stats.started(13).succeeded(13).failed(0));

		Set<String> testNames = payloads.stream()//
				.map(payload -> payload.substring(0, payload.indexOf("-")))//
				.collect(Collectors.toSet());

		assertThat(payloads).hasSize(10);
		assertThat(testNames).hasSize(10);

		// The following assertion is currently commented out, since it fails
		// regularly on the CI server due to only 1 thread being used for
		// parallel test execution on the CI server.
		/*
		Set<String> threadNames = payloads.stream()//
				.map(payload -> payload.substring(payload.indexOf("-")))//
				.collect(Collectors.toSet());
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		// Skip the following assertion entirely if too few processors are available
		// to the current JVM.
		if (availableProcessors >= 6) {
			// There are probably 10 different thread names on a developer's machine,
			// but we really just want to assert that at least two different threads
			// were used, since the CI server often has fewer threads available.
			assertThat(threadNames)
				.as("number of threads used with " + availableProcessors + " available processors")
				.hasSizeGreaterThanOrEqualTo(2);
		}
		*/
	}


	@AfterEach
	void resetPayloads() {
		payloads.clear();
	}


	@SpringJUnitConfig
	@RecordApplicationEvents
	@TestInstance(Lifecycle.PER_METHOD)
	static class TestInstancePerMethodTestCase {

		@Autowired
		ApplicationContext context;

		@Autowired
		ApplicationEvents events;

		@Test
		void test1(TestInfo testInfo) {
			assertTestExpectations(this.events, testInfo);
		}

		@Test
		void test2(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test3(TestInfo testInfo) {
			assertTestExpectations(this.events, testInfo);
		}

		@Test
		void test4(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test5(TestInfo testInfo) {
			assertTestExpectations(this.events, testInfo);
		}

		@Test
		void test6(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test7(TestInfo testInfo) {
			assertTestExpectations(this.events, testInfo);
		}

		@Test
		void test8(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test9(TestInfo testInfo) {
			assertTestExpectations(this.events, testInfo);
		}

		@Test
		void test10(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void compareToApplicationEventsHolder(ApplicationEvents applicationEvents) {
			ApplicationEvents fromThreadHolder = ApplicationEventsHolder.getRequiredApplicationEvents();
			assertThat(fromThreadHolder.stream())
					.hasSameElementsAs(this.events.stream().toList())
					.hasSameElementsAs(applicationEvents.stream().toList());
		}

		@Test
		void asyncPublication(ApplicationEvents events) throws InterruptedException {
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.execute(() -> this.context.publishEvent("asyncPublication"));
			executorService.shutdown();
			executorService.awaitTermination(10, TimeUnit.SECONDS);

			assertThat(events.stream().filter(e -> !(e instanceof TestContextEvent))
					.map(e -> (e instanceof PayloadApplicationEvent<?> pae ? pae.getPayload().toString() : e.toString())))
					.containsExactly("asyncPublication");
		}

		@Test
		void asyncConsumption() {
			this.context.publishEvent("asyncConsumption");

			Awaitility.await().atMost(Durations.ONE_SECOND).untilAsserted(() ->//
					assertThat(ApplicationEventsHolder//
							.getRequiredApplicationEvents()//
							.stream()//
							.filter(e -> !(e instanceof TestContextEvent))//
							.map(e -> (e instanceof PayloadApplicationEvent<?> pae ? pae.getPayload().toString() : e.toString()))//
					).containsExactly("asyncConsumption"));
		}

		private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
			String testName = testInfo.getTestMethod().get().getName();
			String threadName = Thread.currentThread().getName();
			String localPayload = testName + "-" + threadName;
			context.publishEvent(localPayload);
			assertPayloads(events.stream(String.class), localPayload);
		}

		private static void assertPayloads(Stream<String> events, String... values) {
			assertThat(events.peek(payloads::add)).extracting(Object::toString).containsExactly(values);
		}

		@Configuration
		static class Config {
		}

	}

	@TestInstance(Lifecycle.PER_CLASS)
	static class TestInstancePerClassTestCase extends TestInstancePerMethodTestCase {
	}

}

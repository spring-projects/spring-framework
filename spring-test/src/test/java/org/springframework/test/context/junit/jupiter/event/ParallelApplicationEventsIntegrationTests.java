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

package org.springframework.test.context.junit.jupiter.event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests that verify parallel execution support for {@link ApplicationEvents}
 * in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
class ParallelApplicationEventsIntegrationTests {

	private static final Set<String> payloads = ConcurrentHashMap.newKeySet();


	@ParameterizedTest
	@ValueSource(classes = {TestInstancePerMethodTestCase.class, TestInstancePerClassTestCase.class})
	void executeTestsInParallel(Class<?> testClass) {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(testClass))//
				.configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
				.configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(10).succeeded(10).failed(0));

		Set<String> testNames = payloads.stream()//
				.map(payload -> payload.substring(0, payload.indexOf("-")))//
				.collect(Collectors.toSet());
		Set<String> threadNames = payloads.stream()//
				.map(payload -> payload.substring(payload.indexOf("-")))//
				.collect(Collectors.toSet());

		assertThat(payloads).hasSize(10);
		assertThat(testNames).hasSize(10);
		// There are probably 10 different thread names, but we really just want
		// to assert that at least a few different threads were used.
		assertThat(threadNames).hasSizeGreaterThanOrEqualTo(4);
	}


	@AfterEach
	void resetPayloads() {
		payloads.clear();
	}


	@SpringJUnitConfig
	@RecordApplicationEvents
	@Execution(ExecutionMode.CONCURRENT)
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

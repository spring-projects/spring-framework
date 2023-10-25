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

import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
@SpringJUnitConfig
@RecordApplicationEvents
class JUnitJupiterApplicationEventsIntegrationTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;


	@Nested
	@TestInstance(PER_METHOD)
	class TestInstancePerMethodTests {

		@BeforeEach
		void beforeEach() {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
			context.publishEvent(new CustomEvent("beforeEach"));
			assertCustomEvents(applicationEvents, "beforeEach");
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
		}

		@Test
		void test1(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
			String testName = testInfo.getTestMethod().get().getName();

			assertEventTypes(events, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent");
			context.publishEvent(new CustomEvent(testName));
			context.publishEvent("payload1");
			context.publishEvent("payload2");
			assertCustomEvents(events, "beforeEach", testName);
			assertPayloads(events.stream(String.class), "payload1", "payload2");
		}

		@AfterEach
		void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
			String testName = testInfo.getTestMethod().get().getName();

			assertEventTypes(events, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent", "PayloadApplicationEvent", "PayloadApplicationEvent",
					"AfterTestExecutionEvent");
			context.publishEvent(new CustomEvent("afterEach"));
			assertCustomEvents(events, "beforeEach", testName, "afterEach");
			assertEventTypes(events, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent", "PayloadApplicationEvent", "PayloadApplicationEvent",
					"AfterTestExecutionEvent", "CustomEvent");
		}
	}

	@Nested
	@TestInstance(PER_METHOD)
	class TestInstancePerMethodWithClearedEventsTests {

		@BeforeEach
		void beforeEach() {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
			context.publishEvent(new CustomEvent("beforeEach"));
			assertCustomEvents(applicationEvents, "beforeEach");
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
			applicationEvents.clear();
			assertThat(applicationEvents.stream()).isEmpty();
		}

		@Test
		void test1(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
			String testName = testInfo.getTestMethod().get().getName();

			assertEventTypes(events, "BeforeTestExecutionEvent");
			context.publishEvent(new CustomEvent(testName));
			assertCustomEvents(events, testName);
			assertEventTypes(events, "BeforeTestExecutionEvent", "CustomEvent");
		}

		@AfterEach
		void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
			events.clear();
			context.publishEvent(new CustomEvent("afterEach"));
			assertCustomEvents(events, "afterEach");
			assertEventTypes(events, "CustomEvent");
		}
	}

	@Nested
	@TestInstance(PER_CLASS)
	class TestInstancePerClassTests {

		private boolean testAlreadyExecuted = false;

		@BeforeEach
		void beforeEach(TestInfo testInfo) {
			if (!testAlreadyExecuted) {
				assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
						"BeforeTestMethodEvent");
			}
			else {
				assertEventTypes(applicationEvents, "BeforeTestMethodEvent");
			}

			context.publishEvent(new CustomEvent("beforeEach"));
			assertCustomEvents(applicationEvents, "beforeEach");

			if (!testAlreadyExecuted) {
				assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
						"BeforeTestMethodEvent", "CustomEvent");
			}
			else {
				assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent");
			}
		}

		@Test
		void test1(ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		@Test
		void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
			assertTestExpectations(events, testInfo);
		}

		private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
			String testName = testInfo.getTestMethod().get().getName();

			if (!testAlreadyExecuted) {
				assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
						"BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent");
			}
			else {
				assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent");
			}

			context.publishEvent(new CustomEvent(testName));
			assertCustomEvents(events, "beforeEach", testName);

			if (!testAlreadyExecuted) {
				assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
						"BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent", "CustomEvent");
			}
			else {
				assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
						"CustomEvent");
			}
		}

		@AfterEach
		void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
			String testName = testInfo.getTestMethod().get().getName();

			if (!testAlreadyExecuted) {
				assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
						"BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent", "CustomEvent",
						"AfterTestExecutionEvent");
			}
			else {
				assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
						"CustomEvent", "AfterTestExecutionEvent");
			}

			context.publishEvent(new CustomEvent("afterEach"));
			assertCustomEvents(events, "beforeEach", testName, "afterEach");

			if (!testAlreadyExecuted) {
				assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
						"BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent", "CustomEvent",
						"AfterTestExecutionEvent", "CustomEvent");
				testAlreadyExecuted = true;
			}
			else {
				assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
						"CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
			}
		}
	}

	@Nested
	@TestInstance(PER_CLASS)
	class AsyncEventTests {

		@Autowired
		ApplicationEvents applicationEvents;

		@Test
		void asyncPublication() throws InterruptedException {
			Thread t = new Thread(() -> context.publishEvent(new CustomEvent("async")));
			t.start();
			t.join();

			assertThat(this.applicationEvents.stream(CustomEvent.class))
					.singleElement()
					.extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
					.isEqualTo("async");
		}

		@Test
		void asyncConsumption() {
			context.publishEvent(new CustomEvent("sync"));

			Awaitility.await().atMost(Durations.ONE_SECOND)
					.untilAsserted(() -> assertThat(this.applicationEvents.stream(CustomEvent.class))
							.singleElement()
							.extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
							.isEqualTo("sync"));
		}

	}


	private static void assertEventTypes(ApplicationEvents applicationEvents, String... types) {
		assertThat(applicationEvents.stream().map(event -> event.getClass().getSimpleName()))
			.containsExactly(types);
	}

	private static void assertPayloads(Stream<String> events, String... values) {
		assertThat(events).extracting(Object::toString).containsExactly(values);
	}

	private static void assertCustomEvents(ApplicationEvents events, String... messages) {
		assertThat(events.stream(CustomEvent.class)).extracting(CustomEvent::getMessage).containsExactly(messages);
	}


	@Configuration
	static class Config {
	}

	@SuppressWarnings("serial")
	static class CustomEvent extends ApplicationEvent {

		private final String message;


		CustomEvent(String message) {
			super(message);
			this.message = message;
		}

		String getMessage() {
			return message;
		}
	}

}

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

package org.springframework.test.context.testng.event;

import java.lang.reflect.Method;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with TestNG.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
@RecordApplicationEvents
class TestNGApplicationEventsIntegrationTests extends AbstractTestNGSpringContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;

	private boolean testAlreadyExecuted = false;


	@BeforeMethod
	void beforeEach() {
		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent");
		}

		context.publishEvent(new CustomEvent("beforeEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach");

		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent");
		}
	}

	@Test
	void test1() {
		assertTestExpectations("test1");
	}

	@Test
	void test2() {
		assertTestExpectations("test2");
	}

	private void assertTestExpectations(String testName) {
		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent");
		}

		context.publishEvent(new CustomEvent(testName));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", testName);

		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
					"CustomEvent");
		}
	}

	@AfterMethod
	void afterEach(Method testMethod) {
		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent", "AfterTestExecutionEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
					"CustomEvent", "AfterTestExecutionEvent");
		}

		context.publishEvent(new CustomEvent("afterEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", testMethod.getName(), "afterEach");

		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
			testAlreadyExecuted = true;
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
					"CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
		}
	}


	private static void assertEventTypes(ApplicationEvents applicationEvents, String... types) {
		assertThat(applicationEvents.stream().map(event -> event.getClass().getSimpleName()))
			.containsExactly(types);
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

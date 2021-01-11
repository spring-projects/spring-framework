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

package org.springframework.test.context.junit4.event;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with JUnit 4.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
@RunWith(SpringRunner.class)
@RecordApplicationEvents
public class JUnit4ApplicationEventsIntegrationTests {

	@Rule
	public final TestName testName = new TestName();

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;


	@Before
	public void beforeEach() {
		assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
		context.publishEvent(new CustomEvent("beforeEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach");
		assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
	}

	@Test
	public void test1() {
		assertTestExpectations("test1");
	}

	@Test
	public void test2() {
		assertTestExpectations("test2");
	}

	private void assertTestExpectations(String testName) {
		assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
				"BeforeTestExecutionEvent");
		context.publishEvent(new CustomEvent(testName));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", testName);
		assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
				"BeforeTestExecutionEvent", "CustomEvent");
	}

	@After
	public void afterEach() {
		assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
			"BeforeTestExecutionEvent", "CustomEvent", "AfterTestExecutionEvent");
		context.publishEvent(new CustomEvent("afterEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", this.testName.getMethodName(), "afterEach");
		assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
			"BeforeTestExecutionEvent", "CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
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

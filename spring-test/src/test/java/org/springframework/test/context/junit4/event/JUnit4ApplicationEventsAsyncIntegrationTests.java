/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.event.JUnit4ApplicationEventsIntegrationTests.CustomEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} that record async events
 * or assert the events from a separate thread, in conjunction with JUnit 4.
 *
 * @author Simon Baslé
 * @since 6.1
 */
@RunWith(SpringRunner.class)
@RecordApplicationEvents
public class JUnit4ApplicationEventsAsyncIntegrationTests {

	@Rule
	public final TestName testName = new TestName();

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;

	@Test
	public void asyncPublication() throws InterruptedException {
		Thread t = new Thread(() -> context.publishEvent(new CustomEvent("async")));
		t.start();
		t.join();

		assertThat(this.applicationEvents.stream(CustomEvent.class))
				.singleElement()
				.extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
				.isEqualTo("async");
	}

	@Test
	public void asyncConsumption() {
		context.publishEvent(new CustomEvent("sync"));

		Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.untilAsserted(() -> assertThat(this.applicationEvents.stream(CustomEvent.class))
						.singleElement()
						.extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
						.isEqualTo("sync"));
	}


	@Configuration
	static class Config {
	}
}

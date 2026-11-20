/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
@SuppressWarnings("deprecation")
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

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.testng.event;

import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.testng.event.TestNGApplicationEventsIntegrationTests.CustomEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} that record async events
 * or assert the events from a separate thread, in conjunction with TestNG.
 *
 * @author Simon Baslé
 * @since 6.1
 */
@RecordApplicationEvents
class TestNGApplicationEventsAsyncIntegrationTests extends AbstractTestNGSpringContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;


	@Test
	void asyncPublication() throws InterruptedException {
		Thread t = new Thread(() -> context.publishEvent(new CustomEvent("asyncPublication")));
		t.start();
		t.join();

		assertThat(this.applicationEvents.stream(CustomEvent.class))
				.singleElement()
				.extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
				.isEqualTo("asyncPublication");
	}

	@Test
	void asyncConsumption() {
		context.publishEvent(new CustomEvent("asyncConsumption"));

		Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.untilAsserted(() -> assertThat(this.applicationEvents.stream(CustomEvent.class))
						.singleElement()
						.extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
						.isEqualTo("asyncConsumption"));
	}


	@Configuration
	static class Config { }
}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.BeforeTestExecutionEvent;
import org.springframework.test.context.event.BeforeTestMethodEvent;
import org.springframework.test.context.event.PrepareTestInstanceEvent;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.event.TestContextEvent;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the example {@link PublishedEvents} extension to the
 * {@link ApplicationEvents} feature.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
@SpringJUnitConfig
@RecordApplicationEvents
@ExtendWith(PublishedEventsExtension.class)
class PublishedEventsIntegrationTests {

	@Test
	void test(PublishedEvents publishedEvents) {
		assertThat(publishedEvents).isNotNull();
		assertThat(publishedEvents.ofType(TestContextEvent.class)).hasSize(3);
		assertThat(publishedEvents.ofType(PrepareTestInstanceEvent.class)).hasSize(1);
		assertThat(publishedEvents.ofType(BeforeTestMethodEvent.class)).hasSize(1);
		assertThat(publishedEvents.ofType(BeforeTestExecutionEvent.class)).hasSize(1);
		assertThat(publishedEvents.ofType(TestContextEvent.class).ofSubType(BeforeTestExecutionEvent.class)).hasSize(1);
	}


	@Configuration
	static class Config {
	}

}

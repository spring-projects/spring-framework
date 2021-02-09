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

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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;

/**
 * Integration tests for {@link MockitoSpyBean @MockitoSpyBean} used during
 * {@code ApplicationContext} refresh.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see MockitoBeanUsedDuringApplicationContextRefreshIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanUsedDuringApplicationContextRefreshIntegrationTests {

	static ContextRefreshedEvent contextRefreshedEvent;

	@MockitoSpyBean
	ContextRefreshedEventProcessor eventProcessor;


	@AfterAll
	static void clearStaticField() {
		contextRefreshedEvent = null;
	}

	@Test
	void test() {
		assertIsSpy(eventProcessor);

		// Ensure that the spy was invoked during ApplicationContext refresh
		// and has not been reset in the interim.
		then(eventProcessor).should().process(same(contextRefreshedEvent));
	}


	@Configuration
	@Import(ContextRefreshedEventListener.class)
	static class Config {

		@Bean
		ContextRefreshedEventProcessor eventProcessor() {
			// Cannot be a lambda expression, since Mockito cannot create a spy for a lambda.
			return new ContextRefreshedEventProcessor() {

				@Override
				public void process(ContextRefreshedEvent event) {
					contextRefreshedEvent = event;
				}
			};
		}
	}

	interface ContextRefreshedEventProcessor {
		void process(ContextRefreshedEvent event);
	}

	// MUST be annotated with @Component, due to EventListenerMethodProcessor.isSpringContainerClass().
	@Component
	record ContextRefreshedEventListener(ContextRefreshedEventProcessor contextRefreshedEventProcessor) {

		@EventListener
		void onApplicationEvent(ContextRefreshedEvent event) {
			this.contextRefreshedEventProcessor.process(event);
		}
	}

}

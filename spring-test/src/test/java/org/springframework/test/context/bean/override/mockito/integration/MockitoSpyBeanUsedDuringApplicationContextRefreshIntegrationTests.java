/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

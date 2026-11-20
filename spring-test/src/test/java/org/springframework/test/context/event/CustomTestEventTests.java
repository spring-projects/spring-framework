/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.CustomTestEventTests.CustomEventPublishingTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * Integration tests for custom event publication via
 * {@link TestContext#publishEvent(java.util.function.Function)}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@ExtendWith(SpringExtension.class)
@TestExecutionListeners(listeners = CustomEventPublishingTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
class CustomTestEventTests {

	private static final List<CustomEvent> events = new ArrayList<>();


	@BeforeEach
	void clearEvents() {
		events.clear();
	}

	@Test
	void customTestEventPublished() {
		assertThat(events).hasSize(1);
		CustomEvent customEvent = events.get(0);
		assertThat(customEvent.getSource()).isEqualTo(getClass());
		assertThat(customEvent.getTestName()).isEqualTo("customTestEventPublished");
	}


	@Configuration
	static class Config {

		@EventListener
		void processCustomEvent(CustomEvent event) {
			events.add(event);
		}
	}

	@SuppressWarnings("serial")
	static class CustomEvent extends ApplicationEvent {

		private final Method testMethod;


		public CustomEvent(Class<?> testClass, Method testMethod) {
			super(testClass);
			this.testMethod = testMethod;
		}

		String getTestName() {
			return this.testMethod.getName();
		}
	}

	static class CustomEventPublishingTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestExecution(TestContext testContext) throws Exception {
			testContext.publishEvent(tc -> new CustomEvent(tc.getTestClass(), tc.getTestMethod()));
		}
	}

}

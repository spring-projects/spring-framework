/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.parallel;

import java.lang.reflect.Parameter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Constants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests which verify that {@code @BeforeEach} and {@code @AfterEach} methods
 * that accept {@code @Autowired} arguments can be executed in parallel without issues
 * regarding concurrent access to the {@linkplain Parameter parameters} of such methods.
 *
 * @author Sam Brannen
 * @since 5.1.3
 */
class ParallelExecutionSpringExtensionTests {

	private static final int NUM_TESTS = 1000;

	@RepeatedTest(value = 10, failureThreshold = 1)
	void runTestsInParallel() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
				.configurationParameter("junit.platform.discovery.issue.severity.critical", "INFO")//
				.configurationParameter(Constants.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME, "*DisabledCondition")//
				.configurationParameter(Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, "true")//
				.configurationParameter(Constants.PARALLEL_CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME, "10")//
				.configurationParameter(Constants.PARALLEL_CONFIG_EXECUTOR_SERVICE_PROPERTY_NAME, "WORKER_THREAD_POOL")
				.selectors(selectClass(TestCase.class))//
				.execute();

		// List failed events in case of errors to get a sense of what failed.
		Events failedEvents = results.allEvents().failed();
		if (failedEvents.count() > 0) {
			failedEvents.debug();
		}

		results.testEvents().assertStatistics(stats -> stats.started(NUM_TESTS).succeeded(NUM_TESTS).failed(0));
	}

	@SpringJUnitConfig
	@Disabled
	static class TestCase {

		@BeforeEach
		void beforeEach(@Autowired ApplicationContext context) {
		}

		@RepeatedTest(NUM_TESTS)
		void repeatedTest(@Autowired ApplicationContext context) {
		}

		@AfterEach
		void afterEach(@Autowired ApplicationContext context) {
		}

		@Configuration
		static class Config {
		}
	}

}

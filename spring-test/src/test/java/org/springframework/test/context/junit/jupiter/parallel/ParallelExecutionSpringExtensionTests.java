/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.parallel;

import java.lang.reflect.Parameter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.*;

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

	@RepeatedTest(10)
	void runTestsInParallel() {
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);

		LauncherDiscoveryRequest request = request()//
				.configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
				.configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
				.selectors(selectClass(TestCase.class))//
				.build();

		launcher.execute(request);

		assertEquals(NUM_TESTS, listener.getSummary().getTestsSucceededCount(),
				"number of tests executed successfully");
	}

	@SpringJUnitConfig
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

/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

/**
 * Tests which indirectly verify that {@link MockitoResetTestExecutionListener} does not
 * attempt to load an application context to reset mocks if the application context
 * is not currently loaded or previously failed to load.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36782">gh-36782</a>
 * @see MockitoResetTestExecutionListenerTests
 * @see MockitoResetTestExecutionListenerWithContextLoadFailureTestNGTests
 */
class MockitoResetTestExecutionListenerWithContextLoadFailureTests {

	@Test
	void contextLoadFailureCausesExpectedTestFailures() {
		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(ContextLoadFailureTestCase.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(2).succeeded(0).failed(2))
				.assertThatEvents()
					.haveExactly(1, event(test("test1"),
							finishedWithFailure(
									instanceOf(IllegalStateException.class),
									message(msg -> msg.startsWith("Failed to load ApplicationContext")))))
					.haveExactly(1, event(test("test2"),
							finishedWithFailure(
									instanceOf(IllegalStateException.class),
									message(msg -> msg.contains("failure threshold")))));
	}


	/**
	 * <p>The {@code @TestExecutionListeners} declaration replaces the default listeners with
	 * only those needed to exercise {@link MockitoResetTestExecutionListener}, ensuring that
	 * no additional listeners call {@code testContext.getApplicationContext()} without a
	 * conditional {@code hasApplicationContext()} check.
	 */
	@SpringJUnitConfig
	@TestExecutionListeners({
		BeanOverrideTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class,
		MockitoResetTestExecutionListener.class
	})
	@TestMethodOrder(MethodName.class)
	static class ContextLoadFailureTestCase {

		@MockitoBean
		ExampleService exampleService;

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}

		@Configuration(proxyBeanMethods = false)
		static class Config {

			@Bean
			String alwaysFails() {
				throw new RuntimeException("Simulated context load failure");
			}
		}
	}

}

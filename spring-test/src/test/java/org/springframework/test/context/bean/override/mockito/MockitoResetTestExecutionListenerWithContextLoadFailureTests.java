/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

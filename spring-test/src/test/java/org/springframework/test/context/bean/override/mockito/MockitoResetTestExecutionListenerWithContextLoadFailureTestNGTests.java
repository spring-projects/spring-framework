/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.testng.TestNG;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.testng.TrackingTestNGTestListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit based integration test which verifies that
 * {@link MockitoResetTestExecutionListener} &mdash; when used in conjunction with
 * Spring's TestNG support &mdash; does not attempt to load an application context
 * to reset mocks if the application context is not currently loaded or previously
 * failed to load.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36782">gh-36782</a>
 * @see MockitoResetTestExecutionListenerWithContextLoadFailureTests
 */
class MockitoResetTestExecutionListenerWithContextLoadFailureTestNGTests {

	/**
	 * <p>NOTE: The {@code @BeforeMethod(alwaysRun = true)} and {@code @AfterMethod(alwaysRun = true)}
	 * lifecycle methods in {@link AbstractTestNGSpringContextTests} are always invoked, even if a
	 * previous lifecycle configuration method failed (for example, due to a context-load failure).
	 */
	@Test
	void contextLoadFailureCausesExpectedTestFailures() {
		TrackingTestNGTestListener listener = new TrackingTestNGTestListener();
		TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] { ContextLoadFailureTestCase.class });
		testNG.setVerbose(0);
		testNG.run();

		assertThat(listener.testStartCount).as("tests started").hasValue(2);
		assertThat(listener.testSuccessCount).as("tests succeeded").hasValue(0);
		assertThat(listener.testFailureCount).as("tests failed").hasValue(0);
		// Before the introduction of hasApplicationContext() checks in
		// MockitoResetTestExecutionListener, the @BeforeMethod and @AfterMethod
		// lifecycle methods in AbstractTestNGSpringContextTests also attempted to
		// load the faulty ApplicationContext, resulting in 5 configuration failures:
		// 1 * @BeforeClass + 2 * @BeforeMethod + 2 * @AfterMethod = 5.
		// With the fix, only the @BeforeClass context-load failure is recorded.
		assertThat(listener.failedConfigurationsCount).as("failed configurations").hasValue(1);
	}


	/**
	 * <p>The {@code @TestExecutionListeners} declaration replaces the default listeners with
	 * only those needed to exercise {@link MockitoResetTestExecutionListener}, ensuring that
	 * no additional listeners call {@code testContext.getApplicationContext()} without a
	 * conditional {@code hasApplicationContext()} check.
	 */
	@ContextConfiguration
	@TestExecutionListeners({
		BeanOverrideTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class,
		MockitoResetTestExecutionListener.class
	})
	static class ContextLoadFailureTestCase extends AbstractTestNGSpringContextTests {

		@MockitoBean
		ExampleService exampleService;

		@org.testng.annotations.Test
		void test1() {
		}

		@org.testng.annotations.Test
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

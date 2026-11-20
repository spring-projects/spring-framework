/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.context.event.annotation.AfterTestExecution;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.event.annotation.BeforeTestExecution;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.event.annotation.PrepareTestInstance;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for the {@link EventPublishingTestExecutionListener} which verify
 * behavior for test context events when {@link DirtiesContext @DirtiesContext}
 * is used.
 *
 * @author Sam Brannen
 * @since 5.3.17
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/27757">gh-27757</a>
 */
class DirtiesContextEventPublishingTests {

	private static final List<Class<? extends TestContextEvent>> events = new ArrayList<>();


	@BeforeEach
	@AfterEach
	void resetEvents() {
		events.clear();
	}

	@Test
	void classLevelDirtiesContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(ClassLevelDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class //
		);
	}

	@Test
	void methodLevelAfterMethodDirtiesContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(MethodLevelAfterMethodDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class //
			// AfterTestClassEvent.class -- missing b/c of @DirtiestContext "after method" at the method level
		);
	}

	@Test
	void methodLevelAfterMethodDirtiesContextWithSubsequentTestMethod() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(MethodLevelAfterMethodDirtiesContextWithSubsequentTestMethodTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			// test1()
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			// test2()
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class // b/c @DirtiestContext is not applied for test2()
		);
	}

	@Test
	void methodLevelBeforeMethodDirtiesContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(MethodLevelBeforeMethodDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class // b/c @DirtiestContext happens "before method" at the method level
		);
	}

	@SpringJUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = class-level")
	@DirtiesContext
	static class ClassLevelDirtiesContextTestCase {

		@Test
		void test() {
		}
	}

	@SpringJUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = method-level-after-method")
	static class MethodLevelAfterMethodDirtiesContextTestCase {

		@Test
		@DirtiesContext
		void test1() {
		}
	}

	@SpringJUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = method-level-after-method-with-subsequent-test-method")
	@TestMethodOrder(DisplayName.class)
	static class MethodLevelAfterMethodDirtiesContextWithSubsequentTestMethodTestCase {

		@Test
		@DirtiesContext
		void test1() {
		}

		@Test
		void test2() {
		}
	}

	@SpringJUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = method-level-before-method")
	static class MethodLevelBeforeMethodDirtiesContextTestCase {

		@Test
		@DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
		void test() {
		}
	}

	@Configuration
	static class Config {

		@BeforeTestClass
		void beforeTestClass(BeforeTestClassEvent e) {
			events.add(e.getClass());
		}

		@PrepareTestInstance
		void prepareTestInstance(PrepareTestInstanceEvent e) {
			events.add(e.getClass());
		}

		@BeforeTestMethod
		void beforeTestMethod(BeforeTestMethodEvent e) {
			events.add(e.getClass());
		}

		@BeforeTestExecution
		public void beforeTestExecution(BeforeTestExecutionEvent e) {
			events.add(e.getClass());
		}

		@AfterTestExecution
		public void afterTestExecution(AfterTestExecutionEvent e) {
			events.add(e.getClass());
		}

		@AfterTestMethod
		public void afterTestMethod(AfterTestMethodEvent e) {
			events.add(e.getClass());
		}

		@AfterTestClass
		public void afterTestClass(AfterTestClassEvent e) {
			events.add(e.getClass());
		}

	}

}

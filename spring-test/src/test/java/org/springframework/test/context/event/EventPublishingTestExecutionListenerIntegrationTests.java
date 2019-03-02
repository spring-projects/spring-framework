/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.event;

import java.lang.annotation.Retention;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.context.event.annotation.AfterTestExecution;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.event.annotation.BeforeTestExecution;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.event.annotation.PrepareTestInstance;
import org.springframework.util.ReflectionUtils;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link EventPublishingTestExecutionListener} and
 * accompanying {@link TestContextEvent} annotations.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
public class EventPublishingTestExecutionListenerIntegrationTests {

	private final TestContextManager testContextManager = new TestContextManager(ExampleTestCase.class);
	private final TestContext testContext = testContextManager.getTestContext();
	private final TestExecutionListener listener = testContext.getApplicationContext().getBean(EventCaptureConfiguration.class).listener();
	private final Object testInstance = new ExampleTestCase();
	private final Method testMethod = ReflectionUtils.findMethod(ExampleTestCase.class, "test1");


	@Before
	public void resetMock() {
		// The mocked listener is a bean in the ApplicationContext that is stored
		// in a static cache by the Spring TestContext Framework.
		reset(listener);
	}

	@Test
	public void beforeTestClassAnnotation() throws Exception {
		testContextManager.beforeTestClass();
		verify(listener, only()).beforeTestClass(testContext);
	}

	@Test
	public void prepareTestInstanceAnnotation() throws Exception {
		testContextManager.prepareTestInstance(testInstance);
		verify(listener, only()).prepareTestInstance(testContext);
	}

	@Test
	public void beforeTestMethodAnnotationWithMatchingCondition() throws Exception {
		testContextManager.beforeTestMethod(testInstance, testMethod);
		verify(listener, only()).beforeTestMethod(testContext);
	}

	@Test
	public void beforeTestMethodAnnotationWithFailingCondition() throws Exception {
		Method testMethod2 = ReflectionUtils.findMethod(ExampleTestCase.class, "test2");
		testContextManager.beforeTestMethod(testInstance, testMethod2);
		verify(listener, never()).beforeTestMethod(testContext);
	}

	@Test
	public void beforeTestExecutionAnnotation() throws Exception {
		testContextManager.beforeTestExecution(testInstance, testMethod);
		verify(listener, only()).beforeTestExecution(testContext);
	}

	@Test
	public void afterTestExecutionAnnotation() throws Exception {
		testContextManager.afterTestExecution(testInstance, testMethod, null);
		verify(listener, only()).afterTestExecution(testContext);
	}

	@Test
	public void afterTestMethodAnnotation() throws Exception {
		testContextManager.afterTestMethod(testInstance, testMethod, null);
		verify(listener, only()).afterTestMethod(testContext);
	}

	@Test
	public void afterTestClassAnnotation() throws Exception {
		testContextManager.afterTestClass();
		verify(listener, only()).afterTestClass(testContext);
	}


	@Configuration
	static class EventCaptureConfiguration {

		@Bean
		public TestExecutionListener listener() {
			return mock(TestExecutionListener.class);
		}

		@BeforeTestClass("#root.event.source.testClass.name matches '.+TestCase'")
		public void beforeTestClass(BeforeTestClassEvent e) throws Exception {
			listener().beforeTestClass(e.getSource());
		}

		@PrepareTestInstance("#a0.testContext.testClass.name matches '.+TestCase'")
		public void prepareTestInstance(PrepareTestInstanceEvent e) throws Exception {
			listener().prepareTestInstance(e.getSource());
		}

		@BeforeTestMethod("#p0.testContext.testMethod.isAnnotationPresent(T(org.springframework.test.context.event.EventPublishingTestExecutionListenerIntegrationTests.Traceable))")
		public void beforeTestMethod(BeforeTestMethodEvent e) throws Exception {
			listener().beforeTestMethod(e.getSource());
		}

		@BeforeTestExecution
		public void beforeTestExecution(BeforeTestExecutionEvent e) throws Exception {
			listener().beforeTestExecution(e.getSource());
		}

		@AfterTestExecution
		public void afterTestExecution(AfterTestExecutionEvent e) throws Exception {
			listener().afterTestExecution(e.getSource());
		}

		@AfterTestMethod("event.testContext.testMethod.isAnnotationPresent(T(org.springframework.test.context.event.EventPublishingTestExecutionListenerIntegrationTests.Traceable))")
		public void afterTestMethod(AfterTestMethodEvent e) throws Exception {
			listener().afterTestMethod(e.getSource());
		}

		@AfterTestClass("#afterTestClassEvent.testContext.testClass.name matches '.+TestCase'")
		public void afterTestClass(AfterTestClassEvent afterTestClassEvent) throws Exception {
			listener().afterTestClass(afterTestClassEvent.getSource());
		}

	}

	@Retention(RUNTIME)
	@interface Traceable {
	}

	@ContextConfiguration(classes = EventCaptureConfiguration.class)
	@TestExecutionListeners(EventPublishingTestExecutionListener.class)
	static class ExampleTestCase {

		@Traceable
		public void test1() {
			/* no-op */
		}

		public void test2() {
			/* no-op */
		}
	}

}

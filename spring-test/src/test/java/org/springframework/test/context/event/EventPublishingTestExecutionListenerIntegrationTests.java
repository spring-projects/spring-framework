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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link EventPublishingTestExecutionListener} and
 * accompanying {@link TestContextEvent} annotations.
 *
 * @author Frank Scheffler
 * @since 5.2
 */
public class EventPublishingTestExecutionListenerIntegrationTests {

	private TestContextManager testContextManager;
	private TestContext testContext;
	private TestExecutionListener listener;
	private Object testInstance;
	private Method testMethod;


	@Before
	public void initialize() {
		TestContextExposingTestContextManager tcm = new TestContextExposingTestContextManager();
		testContextManager = tcm;
		testContext = tcm.getProtectedTestContext();
		listener = testContext.getApplicationContext().getBean(EventCaptureConfiguration.class).trigger();
		// reset because mock is a cached context bean
		reset(listener);
		testInstance = new EmptyTestCase();
		testMethod = ReflectionUtils.findMethod(EmptyTestCase.class, "dummyMethod");
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
	public void beforeTestMethodAnnotation() throws Exception {
		testContextManager.beforeTestMethod(testInstance, testMethod);
		verify(listener, only()).beforeTestMethod(testContext);
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
		public TestExecutionListener trigger() {
			return mock(TestExecutionListener.class);
		}

		@BeforeTestClass
		public void beforeTestClass(BeforeTestClassEvent e) throws Exception {
			trigger().beforeTestClass(e.getSource());
		}

		@PrepareTestInstance
		public void prepareTestInstance(PrepareTestInstanceEvent e) throws Exception {
			trigger().prepareTestInstance(e.getSource());
		}

		@BeforeTestMethod
		public void beforeTestMethod(BeforeTestMethodEvent e) throws Exception {
			trigger().beforeTestMethod(e.getSource());
		}

		@BeforeTestExecution
		public void beforeTestExecutiob(BeforeTestExecutionEvent e) throws Exception {
			trigger().beforeTestExecution(e.getSource());
		}

		@AfterTestExecution
		public void afterTestExecution(AfterTestExecutionEvent e) throws Exception {
			trigger().afterTestExecution(e.getSource());
		}

		@AfterTestMethod
		public void afterTestMethod(AfterTestMethodEvent e) throws Exception {
			trigger().afterTestMethod(e.getSource());
		}

		@AfterTestClass
		public void afterTestClass(AfterTestClassEvent e) throws Exception {
			trigger().afterTestClass(e.getSource());
		}

	}

	@ContextConfiguration(classes = EventCaptureConfiguration.class)
	@TestExecutionListeners(EventPublishingTestExecutionListener.class)
	static class EmptyTestCase {

		/**
		 * Serves as dummy test method.
		 */
		@SuppressWarnings("PMD.UncommentedEmptyMethodBody")
		public void dummyTestMethod() {
		}
	}

	static class TestContextExposingTestContextManager extends TestContextManager {

		public TestContextExposingTestContextManager() {
			super(EmptyTestCase.class);
		}

		public TestContext getProtectedTestContext() {
			return getTestContext();
		}
	}

}

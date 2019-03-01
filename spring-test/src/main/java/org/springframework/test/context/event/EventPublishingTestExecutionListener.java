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

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}
 * that publishes test lifecycle events to a Spring test
 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
 *
 * <p>These events may be consumed for various reasons, such as resetting <em>mock</em>
 * beans or tracing test execution. Since these events may be consumed by regular
 * Spring beans, they can be shared among different test classes.
 *
 * <h3>Supported Events</h3>
 * <ul>
 * <li>{@link BeforeTestClassEvent}</li>
 * <li>{@link PrepareTestInstanceEvent}</li>
 * <li>{@link BeforeTestMethodEvent}</li>
 * <li>{@link BeforeTestExecutionEvent}</li>
 * <li>{@link AfterTestExecutionEvent}</li>
 * <li>{@link AfterTestMethodEvent}</li>
 * <li>{@link AfterTestClassEvent}</li>
 * </ul>
 *
 * <p>Note that this {@code TestExecutionListener} is not registered by default,
 * but it may be registered for a given test class via
 * {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners}
 * or globally via the {@link org.springframework.core.io.support.SpringFactoriesLoader
 * SpringFactoriesLoader} mechanism (consult the Javadoc and Spring reference manual for
 * details).
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 * @see org.springframework.test.context.event.annotation.BeforeTestClass @BeforeTestClass
 * @see org.springframework.test.context.event.annotation.PrepareTestInstance @PrepareTestInstance
 * @see org.springframework.test.context.event.annotation.BeforeTestMethod @BeforeTestMethod
 * @see org.springframework.test.context.event.annotation.BeforeTestExecution @BeforeTestExecution
 * @see org.springframework.test.context.event.annotation.AfterTestExecution @AfterTestExecution
 * @see org.springframework.test.context.event.annotation.AfterTestMethod @AfterTestMethod
 * @see org.springframework.test.context.event.annotation.AfterTestClass @AfterTestClass
 */
public class EventPublishingTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Returns {@link Ordered#HIGHEST_PRECEDENCE}.
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	/**
	 * Publishes a {@link BeforeTestClassEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void beforeTestClass(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new BeforeTestClassEvent(testContext));
	}

	/**
	 * Publishes a {@link PrepareTestInstanceEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new PrepareTestInstanceEvent(testContext));
	}

	/**
	 * Publishes a {@link BeforeTestMethodEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new BeforeTestMethodEvent(testContext));
	}

	/**
	 * Publishes a {@link BeforeTestExecutionEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void beforeTestExecution(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new BeforeTestExecutionEvent(testContext));
	}

	/**
	 * Publishes an {@link AfterTestExecutionEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void afterTestExecution(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new AfterTestExecutionEvent(testContext));
	}

	/**
	 * Publishes an {@link AfterTestMethodEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new AfterTestMethodEvent(testContext));
	}

	/**
	 * Publishes an {@link AfterTestClassEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) {
		testContext.getApplicationContext().publishEvent(new AfterTestClassEvent(testContext));
	}

}

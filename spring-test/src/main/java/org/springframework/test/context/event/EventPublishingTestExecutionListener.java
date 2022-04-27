/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.event;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} that publishes test execution events to the
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for the currently executing test.
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
 * <p>These events may be consumed for various reasons, such as resetting <em>mock</em>
 * beans or tracing test execution. One advantage of consuming test events rather
 * than implementing a custom {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListener} is that test events may be consumed by any Spring bean
 * registered in the test {@code ApplicationContext}, and such beans may benefit
 * directly from dependency injection and other features of the {@code ApplicationContext}.
 * In contrast, a {@code TestExecutionListener} is not a bean in the {@code ApplicationContext}.
 *
 * <p>Note that the {@code EventPublishingTestExecutionListener} is registered by
 * default; however, it only publishes events if the {@code ApplicationContext}
 * {@linkplain TestContext#hasApplicationContext() has already been loaded}. This
 * prevents the {@code ApplicationContext} from being loaded unnecessarily or too
 * early. Consequently, a {@code BeforeTestClassEvent} will not be published until
 * after the {@code ApplicationContext} has been loaded by another
 * {@code TestExecutionListener}. For example, with the default set of
 * {@code TestExecutionListeners} registered, a {@code BeforeTestClassEvent} will
 * not be published for the first test class that uses a particular test
 * {@code ApplicationContext}, but a {@code BeforeTestClassEvent} will be published
 * for any subsequent test class in the same test suite that uses the same test
 * {@code ApplicationContext} since the context will already have been loaded
 * when subsequent test classes run (as long as the context has not been removed
 * from the {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * via {@link org.springframework.test.annotation.DirtiesContext @DirtiesContext}
 * or the max-size eviction policy). If you wish to ensure that a
 * {@code BeforeTestClassEvent} is published for every test class, you need to
 * register a {@code TestExecutionListener} that loads the {@code ApplicationContext}
 * in the {@link org.springframework.test.context.TestExecutionListener#beforeTestClass
 * beforeTestClass} callback, and that {@code TestExecutionListener} must be registered
 * before the {@code EventPublishingTestExecutionListener}. Similarly, if
 * {@code @DirtiesContext} is used to remove the {@code ApplicationContext} from
 * the context cache after the last test method in a given test class, the
 * {@code AfterTestClassEvent} will not be published for that test class.
 *
 * <h3>Exception Handling</h3>
 * <p>By default, if a test event listener throws an exception while consuming
 * a test event, that exception will propagate to the underlying testing framework
 * in use. For example, if the consumption of a {@code BeforeTestMethodEvent}
 * results in an exception, the corresponding test method will fail as a result
 * of the exception. In contrast, if an asynchronous test event listener throws
 * an exception, the exception will not propagate to the underlying testing framework.
 * For further details on asynchronous exception handling, consult the class-level
 * Javadoc for {@link org.springframework.context.event.EventListener @EventListener}.
 *
 * <h3>Asynchronous Listeners</h3>
 * <p>If you want a particular test event listener to process events asynchronously,
 * you can use Spring's {@link org.springframework.scheduling.annotation.Async @Async}
 * support. For further details, consult the class-level Javadoc for
 * {@link org.springframework.context.event.EventListener @EventListener}.
 *
 * @author Sam Brannen
 * @author Frank Scheffler
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
	 * Returns {@code 10000}.
	 */
	@Override
	public final int getOrder() {
		return 10_000;
	}

	/**
	 * Publish a {@link BeforeTestClassEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void beforeTestClass(TestContext testContext) {
		testContext.publishEvent(BeforeTestClassEvent::new);
	}

	/**
	 * Publish a {@link PrepareTestInstanceEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) {
		testContext.publishEvent(PrepareTestInstanceEvent::new);
	}

	/**
	 * Publish a {@link BeforeTestMethodEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) {
		testContext.publishEvent(BeforeTestMethodEvent::new);
	}

	/**
	 * Publish a {@link BeforeTestExecutionEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void beforeTestExecution(TestContext testContext) {
		testContext.publishEvent(BeforeTestExecutionEvent::new);
	}

	/**
	 * Publish an {@link AfterTestExecutionEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void afterTestExecution(TestContext testContext) {
		testContext.publishEvent(AfterTestExecutionEvent::new);
	}

	/**
	 * Publish an {@link AfterTestMethodEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) {
		testContext.publishEvent(AfterTestMethodEvent::new);
	}

	/**
	 * Publish an {@link AfterTestClassEvent} to the {@code ApplicationContext}
	 * for the supplied {@link TestContext}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) {
		testContext.publishEvent(AfterTestClassEvent::new);
	}

}

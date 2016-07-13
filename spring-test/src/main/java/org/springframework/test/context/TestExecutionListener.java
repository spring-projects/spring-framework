/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context;

/**
 * {@code TestExecutionListener} defines a <em>listener</em> API for reacting to
 * test execution events published by the {@link TestContextManager} with which
 * the listener is registered.
 *
 * <p>This interface provides empty {@code default} implementations for all methods.
 * Concrete implementations can therefore choose to override only those methods
 * suitable for the task at hand.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor,
 * so that listeners can be instantiated transparently by tools and configuration
 * mechanisms.
 *
 * <p>Implementations may optionally declare the position in which they should
 * be ordered among the chain of default listeners via the
 * {@link org.springframework.core.Ordered Ordered} interface or
 * {@link org.springframework.core.annotation.Order @Order} annotation. See
 * {@link TestContextBootstrapper#getTestExecutionListeners()} for details.
 *
 * <p>Spring provides the following out-of-the-box implementations (all of
 * which implement {@code Ordered}):
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener
 * ServletTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
 * DirtiesContextBeforeModesTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener
 * DependencyInjectionTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener
 * DirtiesContextTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * TransactionalTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
 * SqlScriptsTestExecutionListener}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see TestContextManager
 * @see org.springframework.test.context.support.AbstractTestExecutionListener
 */
public interface TestExecutionListener {

	/**
	 * Pre-processes a test class <em>before</em> execution of all tests within
	 * the class.
	 * <p>This method should be called immediately before framework-specific
	 * <em>before class</em> lifecycle callbacks.
	 * <p>If a given testing framework does not support <em>before class</em>
	 * lifecycle callbacks, this method will not be called for that framework.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context for the test; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 3.0
	 */
	default void beforeTestClass(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * Prepares the {@link Object test instance} of the supplied
	 * {@link TestContext test context}, for example by injecting dependencies.
	 * <p>This method should be called immediately after instantiation of the test
	 * instance but prior to any framework-specific lifecycle callbacks.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context for the test; never {@code null}
	 * @throws Exception allows any exception to propagate
	 */
	default void prepareTestInstance(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * Pre-processes a test <em>before</em> execution of <em>before</em>
	 * lifecycle callbacks of the underlying test framework &mdash; for example,
	 * by setting up test fixtures.
	 * <p>This method <strong>must</strong> be called immediately prior to
	 * framework-specific <em>before</em> lifecycle callbacks. For historical
	 * reasons, this method is named {@code beforeTestMethod}. Since the
	 * introduction of {@link #beforeTestExecution}, a more suitable name for
	 * this method might be something like {@code beforeTestSetUp} or
	 * {@code beforeEach}; however, it is unfortunately impossible to rename
	 * this method due to backward compatibility concerns.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context in which the test method will be
	 * executed; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 */
	default void beforeTestMethod(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * Pre-processes a test <em>immediately before</em> execution of the
	 * {@link java.lang.reflect.Method test method} in the supplied
	 * {@link TestContext test context} &mdash; for example, for timing
	 * or logging purposes.
	 * <p>This method <strong>must</strong> be called after framework-specific
	 * <em>before</em> lifecycle callbacks.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context in which the test method will be
	 * executed; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #afterTestExecution
	 */
	default void beforeTestExecution(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * Post-processes a test <em>immediately after</em> execution of the
	 * {@link java.lang.reflect.Method test method} in the supplied
	 * {@link TestContext test context} &mdash; for example, for timing
	 * or logging purposes.
	 * <p>This method <strong>must</strong> be called before framework-specific
	 * <em>after</em> lifecycle callbacks.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context in which the test method will be
	 * executed; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 */
	default void afterTestExecution(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * Post-processes a test <em>after</em> execution of <em>after</em>
	 * lifecycle callbacks of the underlying test framework &mdash; for example,
	 * by tearing down test fixtures.
	 * <p>This method <strong>must</strong> be called immediately after
	 * framework-specific <em>after</em> lifecycle callbacks. For historical
	 * reasons, this method is named {@code afterTestMethod}. Since the
	 * introduction of {@link #afterTestExecution}, a more suitable name for
	 * this method might be something like {@code afterTestTearDown} or
	 * {@code afterEach}; however, it is unfortunately impossible to rename
	 * this method due to backward compatibility concerns.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context in which the test method was
	 * executed; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @see #beforeTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 */
	default void afterTestMethod(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * Post-processes a test class <em>after</em> execution of all tests within
	 * the class.
	 * <p>This method should be called immediately after framework-specific
	 * <em>after class</em> lifecycle callbacks.
	 * <p>If a given testing framework does not support <em>after class</em>
	 * lifecycle callbacks, this method will not be called for that framework.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context for the test; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 3.0
	 */
	default void afterTestClass(TestContext testContext) throws Exception {
		/* no-op */
	}

}

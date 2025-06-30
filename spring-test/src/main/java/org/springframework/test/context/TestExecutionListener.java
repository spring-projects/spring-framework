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

package org.springframework.test.context;

/**
 * {@code TestExecutionListener} defines a <em>listener</em> API for reacting to
 * test execution events published by the {@link TestContextManager} with which
 * the listener is registered.
 *
 * <p>Note that not all testing frameworks support all lifecycle callbacks defined
 * in this API. For example, the {@link #beforeTestExecution(TestContext)
 * beforeTestExecution} and {@link #afterTestExecution(TestContext)
 * afterTestExecution} callbacks are not supported in conjunction with JUnit 4 when
 * using the {@link org.springframework.test.context.junit4.rules.SpringMethodRule
 * SpringMethodRule}.
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
 * <h3>Wrapping Behavior for Listeners</h3>
 *
 * <p>The {@link TestContextManager} guarantees <em>wrapping</em> behavior for
 * multiple registered listeners that implement lifecycle callbacks such as
 * {@link #beforeTestClass(TestContext) beforeTestClass},
 * {@link #afterTestClass(TestContext) afterTestClass},
 * {@link #beforeTestMethod(TestContext) beforeTestMethod},
 * {@link #afterTestMethod(TestContext) afterTestMethod},
 * {@link #beforeTestExecution(TestContext) beforeTestExecution}, and
 * {@link #afterTestExecution(TestContext) afterTestExecution}. This means that,
 * given two listeners {@code Listener1} and {@code Listener2} with {@code Listener1}
 * registered before {@code Listener2}, any <em>before</em> callbacks implemented
 * by {@code Listener1} are guaranteed to be invoked <strong>before</strong> any
 * <em>before</em> callbacks implemented by {@code Listener2}. Similarly, given
 * the same two listeners registered in the same order, any <em>after</em>
 * callbacks implemented by {@code Listener1} are guaranteed to be invoked
 * <strong>after</strong> any <em>after</em> callbacks implemented by
 * {@code Listener2}. {@code Listener1} is therefore said to <em>wrap</em>
 * {@code Listener2}.
 *
 * <p>For a concrete example, consider the relationship between the
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * TransactionalTestExecutionListener} and the
 * {@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
 * SqlScriptsTestExecutionListener}. The {@code SqlScriptsTestExecutionListener}
 * is registered after the {@code TransactionalTestExecutionListener}, so that
 * SQL scripts are executed within a transaction managed by the
 * {@code TransactionalTestExecutionListener}.
 *
 * <h3>Registering TestExecutionListener Implementations</h3>
 *
 * <p>A {@code TestExecutionListener} can be registered explicitly for a test class,
 * its subclasses, and its nested classes by using the
 * {@link TestExecutionListeners @TestExecutionListeners} annotation. Explicit
 * registration is suitable for custom listeners that are used in limited testing
 * scenarios. However, it can become cumbersome if a custom listener needs to be
 * used across an entire test suite. This issue is addressed through support for
 * automatic discovery of <em>default</em> {@code TestExecutionListener}
 * implementations through the
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * mechanism. Specifically, default {@code TestExecutionListener} implementations
 * can be registered under the {@code org.springframework.test.context.TestExecutionListener}
 * key in a {@code META-INF/spring.factories} properties file.
 *
 * <p>Spring provides the following implementations. Each of these implements
 * {@code Ordered} and is registered automatically by default.
 *
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener
 * ServletTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
 * DirtiesContextBeforeModesTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.event.ApplicationEventsTestExecutionListener
 * ApplicationEventsTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener
 * BeanOverrideTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener
 * DependencyInjectionTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.observation.MicrometerObservationRegistryTestExecutionListener
 * MicrometerObservationRegistryTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener
 * DirtiesContextTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.CommonCachesTestExecutionListener
 * CommonCachesTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * TransactionalTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
 * SqlScriptsTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.event.EventPublishingTestExecutionListener
 * EventPublishingTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.bean.override.mockito.MockitoResetTestExecutionListener
 * MockitoResetTestExecutionListener}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see TestExecutionListeners @TestExecutionListeners
 * @see TestContextManager
 * @see org.springframework.test.context.support.AbstractTestExecutionListener
 */
public interface TestExecutionListener {

	/**
	 * Pre-processes a test class <em>before</em> execution of all tests within
	 * the class.
	 * <p>This method should be called immediately before framework-specific
	 * <em>before class</em> lifecycle callbacks.
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for lifecycle callbacks.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context for the test; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 3.0
	 */
	default void beforeTestClass(TestContext testContext) throws Exception {
	}

	/**
	 * Prepares the {@linkplain Object test instance} of the supplied
	 * {@linkplain TestContext test context} &mdash; for example, to inject
	 * dependencies.
	 * <p>This method should be called immediately after instantiation of the test
	 * class or as soon after instantiation as possible (as is the case with the
	 * {@link org.springframework.test.context.junit4.rules.SpringMethodRule
	 * SpringMethodRule}). In any case, this method must be called prior to any
	 * framework-specific lifecycle callbacks.
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for listeners.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context for the test; never {@code null}
	 * @throws Exception allows any exception to propagate
	 */
	default void prepareTestInstance(TestContext testContext) throws Exception {
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
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for lifecycle callbacks.
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
	}

	/**
	 * Pre-processes a test <em>immediately before</em> execution of the
	 * {@linkplain java.lang.reflect.Method test method} in the supplied
	 * {@linkplain TestContext test context} &mdash; for example, for timing
	 * or logging purposes.
	 * <p>This method <strong>must</strong> be called after framework-specific
	 * <em>before</em> lifecycle callbacks.
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for lifecycle callbacks.
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
	}

	/**
	 * Post-processes a test <em>immediately after</em> execution of the
	 * {@linkplain java.lang.reflect.Method test method} in the supplied
	 * {@linkplain TestContext test context} &mdash; for example, for timing
	 * or logging purposes.
	 * <p>This method <strong>must</strong> be called before framework-specific
	 * <em>after</em> lifecycle callbacks.
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for lifecycle callbacks.
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
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for lifecycle callbacks.
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
	}

	/**
	 * Post-processes a test class <em>after</em> execution of all tests within
	 * the class.
	 * <p>This method should be called immediately after framework-specific
	 * <em>after class</em> lifecycle callbacks.
	 * <p>See the {@linkplain TestExecutionListener class-level documentation}
	 * for details on wrapping behavior for lifecycle callbacks.
	 * <p>The default implementation is <em>empty</em>. Can be overridden by
	 * concrete classes as necessary.
	 * @param testContext the test context for the test; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 3.0
	 */
	default void afterTestClass(TestContext testContext) throws Exception {
	}

}

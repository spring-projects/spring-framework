/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.AttributeAccessor;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code TestContext} encapsulates the context in which a test is executed,
 * agnostic of the actual testing framework in use.
 *
 * <p>Concrete implementations are highly encouraged to implement a <em>copy
 * constructor</em> in order to allow the immutable state and attributes of a
 * {@code TestContext} to be used as a template for additional contexts created
 * for parallel test execution. The copy constructor must accept a single argument
 * of the type of the concrete implementation. Any implementation that does not
 * provide a copy constructor will likely fail in an environment that executes
 * tests concurrently.
 *
 * <p>As of Spring Framework 6.1, concrete implementations are highly encouraged to
 * override {@link #setMethodInvoker(MethodInvoker)} and {@link #getMethodInvoker()}.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see TestContextManager
 * @see TestExecutionListener
 */
public interface TestContext extends AttributeAccessor, Serializable {

	/**
	 * Determine if the {@linkplain ApplicationContext application context} for
	 * this test context is known to be available.
	 * <p>If this method returns {@code true}, a subsequent invocation of
	 * {@link #getApplicationContext()} should succeed.
	 * <p>The default implementation of this method always returns {@code false}.
	 * Custom {@code TestContext} implementations are therefore highly encouraged
	 * to override this method with a more meaningful implementation. Note that
	 * the standard {@code TestContext} implementation in Spring overrides this
	 * method appropriately.
	 * @return {@code true} if the application context has already been loaded
	 * @since 5.2
	 * @see #getApplicationContext()
	 */
	default boolean hasApplicationContext() {
		return false;
	}

	/**
	 * Get the {@linkplain ApplicationContext application context} for this
	 * test context, possibly cached.
	 * <p>Implementations of this method are responsible for loading the
	 * application context if the corresponding context has not already been
	 * loaded, potentially caching the context as well.
	 * @return the application context (never {@code null})
	 * @throws IllegalStateException if an error occurs while retrieving the
	 * application context
	 * @see #hasApplicationContext()
	 */
	ApplicationContext getApplicationContext();

	/**
	 * Publish the {@link ApplicationEvent} created by the given {@code eventFactory}
	 * to the {@linkplain ApplicationContext application context} for this
	 * test context.
	 * <p>The {@code ApplicationEvent} will only be published if the application
	 * context for this test context {@linkplain #hasApplicationContext() is available}.
	 * @param eventFactory factory for lazy creation of the {@code ApplicationEvent}
	 * @since 5.2
	 * @see #hasApplicationContext()
	 * @see #getApplicationContext()
	 */
	default void publishEvent(Function<TestContext, ? extends ApplicationEvent> eventFactory) {
		if (hasApplicationContext()) {
			getApplicationContext().publishEvent(eventFactory.apply(this));
		}
	}

	/**
	 * Get the {@linkplain Class test class} for this test context.
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * Get the current {@linkplain Object test instance} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test instance (never {@code null})
	 * @see #updateState(Object, Method, Throwable)
	 */
	Object getTestInstance();

	/**
	 * Get the current {@linkplain Method test method} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test method (never {@code null})
	 * @see #updateState(Object, Method, Throwable)
	 */
	Method getTestMethod();

	/**
	 * Get the {@linkplain Throwable exception} that was thrown during execution
	 * of the {@linkplain #getTestMethod() test method}.
	 * <p>Note: this is a mutable property.
	 * @return the exception that was thrown, or {@code null} if no exception was thrown
	 * @see #updateState(Object, Method, Throwable)
	 */
	@Nullable
	Throwable getTestException();

	/**
	 * Call this method to signal that the {@linkplain ApplicationContext application
	 * context} associated with this test context is <em>dirty</em> and should be
	 * removed from the context cache.
	 * <p>Do this if a test has modified the context &mdash; for example, by
	 * modifying the state of a singleton bean, modifying the state of an embedded
	 * database, etc.
	 * @param hierarchyMode the context cache clearing mode to be applied if the
	 * context is part of a hierarchy (may be {@code null})
	 */
	void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode);

	/**
	 * Update this test context to reflect the state of the currently executing test.
	 * <p><strong>WARNING</strong>: This method should only be invoked by the
	 * {@link TestContextManager}.
	 * <p>Caution: concurrent invocations of this method might not be thread-safe,
	 * depending on the underlying implementation.
	 * @param testInstance the current test instance (may be {@code null})
	 * @param testMethod the current test method (may be {@code null})
	 * @param testException the exception that was thrown in the test method,
	 * or {@code null} if no exception was thrown
	 */
	void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException);

	/**
	 * Set the {@link MethodInvoker} to use.
	 * <p>By default, this method does nothing.
	 * <p>Concrete implementations should track the supplied {@code MethodInvoker}
	 * and return it from {@link #getMethodInvoker()}. Note that the standard
	 * {@code TestContext} implementation in Spring overrides this method appropriately.
	 * @since 6.1
	 */
	default void setMethodInvoker(MethodInvoker methodInvoker) {
		/* no-op */
	}

	/**
	 * Get the {@link MethodInvoker} to use.
	 * <p>By default, this method returns {@link MethodInvoker#DEFAULT_INVOKER}.
	 * <p>Concrete implementations should return the {@code MethodInvoker} supplied
	 * to {@link #setMethodInvoker(MethodInvoker)}. Note that the standard
	 * {@code TestContext} implementation in Spring overrides this method appropriately.
	 * @since 6.1
	 */
	default MethodInvoker getMethodInvoker() {
		return MethodInvoker.DEFAULT_INVOKER;
	}

}

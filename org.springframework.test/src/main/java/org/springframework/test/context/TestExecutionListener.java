/*
 * Copyright 2002-2008 the original author or authors.
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
 * <p>
 * <code>TestExecutionListener</code> defines a <em>listener</em> API for
 * reacting to test execution events published by the {@link TestContextManager}
 * with which the listener is registered.
 * </p>
 * <p>
 * Concrete implementations must provide a <code>public</code> no-args
 * constructor, so that listeners can be instantiated transparently by tools and
 * configuration mechanisms.
 * </p>
 * <p>
 * Spring provides the following out-of-the-box implementations:
 * </p>
 * <ul>
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener DependencyInjectionTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener}</li>
 * <li>{@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface TestExecutionListener {

	/**
	 * Prepares the {@link Object test instance} of the supplied
	 * {@link TestContext test context}, for example for injecting
	 * dependencies.
	 * <p>This method should be called immediately after instantiation but prior to
	 * any framework-specific lifecycle callbacks.
	 * @param testContext the test context for the test (never <code>null</code>)
	 * @throws Exception allows any exception to propagate
	 */
	void prepareTestInstance(TestContext testContext) throws Exception;

	/**
	 * Pre-processes a test just <em>before</em> execution of the
	 * {@link java.lang.reflect.Method test method} in the supplied
	 * {@link TestContext test context}, for example for setting up test
	 * fixtures.
	 * @param testContext the test context in which the test method will be
	 * executed (never <code>null</code>)
	 * @throws Exception allows any exception to propagate
	 */
	void beforeTestMethod(TestContext testContext) throws Exception;

	/**
	 * Post-processes a test just <em>after</em> execution of the
	 * {@link java.lang.reflect.Method test method} in the supplied
	 * {@link TestContext test context}, for example for tearing down test
	 * fixtures.
	 * @param testContext the test context in which the test method was
	 * executed (never <code>null</code>)
	 * @throws Exception allows any exception to propagate
	 */
	void afterTestMethod(TestContext testContext) throws Exception;

}

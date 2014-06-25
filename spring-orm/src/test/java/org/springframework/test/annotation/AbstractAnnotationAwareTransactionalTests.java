/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

/**
 * This class is only used within tests in the spring-orm module.
 *
 * <p>Java 5 specific subclass of
 * {@link AbstractTransactionalDataSourceSpringContextTests}, obeying annotations
 * for transaction control.
 *
 * <p>For example, test methods can be annotated with the regular Spring
 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
 * annotation &mdash; for example, to force execution in a read-only transaction
 * or to prevent any transaction from being created at all by setting the propagation
 * level to {@code NOT_SUPPORTED}.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of using the listener-based TestContext framework
 */
@Deprecated
public abstract class AbstractAnnotationAwareTransactionalTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	private final TransactionAttributeSource transactionAttributeSource = new AnnotationTransactionAttributeSource();


	/**
	 * Default constructor for AbstractAnnotationAwareTransactionalTests, which
	 * delegates to {@link #AbstractAnnotationAwareTransactionalTests(String)}.
	 */
	public AbstractAnnotationAwareTransactionalTests() {
		this(null);
	}

	/**
	 * Constructs a new AbstractAnnotationAwareTransactionalTests instance with
	 * the specified JUnit {@code name}.
	 * @param name the name of the current test
	 */
	public AbstractAnnotationAwareTransactionalTests(String name) {
		super(name);
	}

	/**
	 * Overridden to populate transaction definition from annotations.
	 */
	@Override
	public void runBare() throws Throwable {
		// getName will return the name of the method being run.
		if (isDisabledInThisEnvironment(getName())) {
			// Let superclass log that we didn't run the test.
			super.runBare();
			return;
		}

		final Method testMethod = getTestMethod();

		TransactionDefinition explicitTransactionDefinition = this.transactionAttributeSource.getTransactionAttribute(
			testMethod, getClass());
		if (explicitTransactionDefinition != null) {
			this.logger.info("Custom transaction definition [" + explicitTransactionDefinition + "] for test method ["
					+ getName() + "].");
			setTransactionDefinition(explicitTransactionDefinition);
		}

		if (this.transactionDefinition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			preventTransaction();
		}

		super.runBare();
	}

	/**
	 * Get the current test method.
	 */
	protected Method getTestMethod() {
		assertNotNull("TestCase.getName() cannot be null", getName());
		Method testMethod = null;
		try {
			// Use same algorithm as JUnit itself to retrieve the test method
			// about to be executed (the method name is returned by getName). It
			// has to be public so we can retrieve it.
			testMethod = getClass().getMethod(getName(), (Class[]) null);
		}
		catch (NoSuchMethodException ex) {
			fail("Method '" + getName() + "' not found");
		}
		if (!Modifier.isPublic(testMethod.getModifiers())) {
			fail("Method '" + getName() + "' should be public");
		}
		return testMethod;
	}

	/**
	 * Determine whether or not to roll back transactions for the current test.
	 * <p>The default implementation simply delegates to {@link #isDefaultRollback()}.
	 * @return the <em>rollback</em> flag for the current test
	 */
	@Override
	protected boolean isRollback() {
		boolean rollback = isDefaultRollback();
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Using default rollback [" + rollback + "] for test [" + getName() + "].");
		}
		return rollback;
	}

}

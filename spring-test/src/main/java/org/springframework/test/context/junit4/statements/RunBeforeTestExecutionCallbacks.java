/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunBeforeTestExecutionCallbacks} is a custom JUnit {@link Statement}
 * which allows the <em>Spring TestContext Framework</em> to be plugged into the
 * JUnit 4 execution chain by calling {@link TestContextManager#beforeTestExecution
 * beforeTestExecution()} on the supplied {@link TestContextManager}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see #evaluate()
 * @see RunAfterTestExecutionCallbacks
 * @deprecated since Spring Framework 7.0 in favor of the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * and JUnit Jupiter
 */
@Deprecated(since = "7.0")
public class RunBeforeTestExecutionCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final Method testMethod;

	private final TestContextManager testContextManager;


	/**
	 * Construct a new {@code RunBeforeTestExecutionCallbacks} statement.
	 * @param next the next {@code Statement} in the execution chain
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which is about to be executed on the
	 * test instance
	 * @param testContextManager the TestContextManager upon which to call
	 * {@code beforeTestExecution()}
	 */
	public RunBeforeTestExecutionCallbacks(Statement next, Object testInstance, Method testMethod,
			TestContextManager testContextManager) {

		this.next = next;
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testContextManager = testContextManager;
	}

	/**
	 * Invoke {@link TestContextManager#beforeTestExecution(Object, Method)}
	 * and then evaluate the next {@link Statement} in the execution chain
	 * (typically an instance of
	 * {@link org.junit.internal.runners.statements.InvokeMethod InvokeMethod}).
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.beforeTestExecution(this.testInstance, this.testMethod);
		this.next.evaluate();
	}

}

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

package org.springframework.test.context.junit4.statements;

import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunAfterTestClassCallbacks} is a custom JUnit {@link Statement} which allows
 * the <em>Spring TestContext Framework</em> to be plugged into the JUnit execution chain
 * by calling {@link TestContextManager#afterTestClass afterTestClass()} on the supplied
 * {@link TestContextManager}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 * @see RunBeforeTestClassCallbacks
 */
public class RunAfterTestClassCallbacks extends Statement {

	private final Statement next;

	private final TestContextManager testContextManager;


	/**
	 * Construct a new {@code RunAfterTestClassCallbacks} statement.
	 * @param next the next {@code Statement} in the execution chain
	 * @param testContextManager the TestContextManager upon which to call
	 * {@code afterTestClass()}
	 */
	public RunAfterTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		this.next = next;
		this.testContextManager = testContextManager;
	}


	/**
	 * Evaluate the next {@link Statement} in the execution chain (typically an instance of
	 * {@link org.junit.internal.runners.statements.RunAfters RunAfters}), catching any
	 * exceptions thrown, and then invoke {@link TestContextManager#afterTestClass()}.
	 * <p>If the invocation of {@code afterTestClass()} throws an exception, it will also
	 * be tracked. Multiple exceptions will be combined into a {@link MultipleFailureException}.
	 */
	@Override
	public void evaluate() throws Throwable {
		List<Throwable> errors = new ArrayList<>();
		try {
			this.next.evaluate();
		}
		catch (Throwable ex) {
			errors.add(ex);
		}

		try {
			this.testContextManager.afterTestClass();
		}
		catch (Throwable ex) {
			errors.add(ex);
		}

		MultipleFailureException.assertEmpty(errors);
	}

}

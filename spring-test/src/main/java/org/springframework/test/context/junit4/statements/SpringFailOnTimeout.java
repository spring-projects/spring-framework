/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.concurrent.TimeoutException;

import org.junit.runners.model.Statement;
import org.springframework.test.annotation.Timed;

/**
 * <code>SpringFailOnTimeout</code> is a custom JUnit 4.5+ {@link Statement}
 * which adds support for Spring's {@link Timed @Timed} annotation by throwing
 * an exception if the next statement in the execution chain takes more than the
 * specified number of milliseconds.
 *
 * @see #evaluate()
 * @author Sam Brannen
 * @since 3.0
 */
public class SpringFailOnTimeout extends Statement {

	private final Statement next;

	private final long timeout;


	/**
	 * Constructs a new <code>SpringFailOnTimeout</code> statement.
	 *
	 * @param next the next <code>Statement</code> in the execution chain
	 * @param timeout the configured <code>timeout</code> for the current test
	 * @see Timed#millis()
	 */
	public SpringFailOnTimeout(Statement next, long timeout) {
		this.next = next;
		this.timeout = timeout;
	}

	/**
	 * Invokes the next {@link Statement statement} in the execution chain
	 * (typically an instance of
	 * {@link org.junit.internal.runners.statements.InvokeMethod InvokeMethod}
	 * or {@link org.junit.internal.runners.statements.ExpectException
	 * ExpectException}) and throws an exception if the next
	 * <code>statement</code> takes more than the specified <code>timeout</code>
	 * .
	 */
	@Override
	public void evaluate() throws Throwable {
		long startTime = System.currentTimeMillis();
		try {
			this.next.evaluate();
		}
		finally {
			long elapsed = System.currentTimeMillis() - startTime;
			if (elapsed > this.timeout) {
				throw new TimeoutException(String.format("Test took %s ms; limit was %s ms.", elapsed, this.timeout));
			}
		}
	}

}

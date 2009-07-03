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

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.Statement;
import org.springframework.test.annotation.Repeat;
import org.springframework.util.ClassUtils;

/**
 * <code>SpringRepeat</code> is a custom JUnit 4.5+ {@link Statement} which adds
 * support for Spring's {@link Repeat &#064;Repeat} annotation by repeating the
 * test for the specified number of times.
 * 
 * @see #evaluate()
 * @author Sam Brannen
 * @since 3.0
 */
public class SpringRepeat extends Statement {

	protected static final Log logger = LogFactory.getLog(SpringRepeat.class);

	private final Statement next;

	private final Method testMethod;

	private final int repeat;


	/**
	 * Constructs a new <code>SpringRepeat</code> statement.
	 * 
	 * @param next the next <code>Statement</code> in the execution chain
	 * @param testMethod the current test method
	 * @param repeat the configured repeat count for the current test method
	 * @see Repeat#value()
	 */
	public SpringRepeat(Statement next, Method testMethod, int repeat) {
		this.next = next;
		this.testMethod = testMethod;
		this.repeat = Math.max(1, repeat);
	}

	/**
	 * Invokes the next {@link Statement statement} in the execution chain for
	 * the specified repeat count.
	 */
	@Override
	public void evaluate() throws Throwable {
		for (int i = 0; i < this.repeat; i++) {
			if (this.repeat > 1 && logger.isInfoEnabled()) {
				logger.info(String.format("Repetition %d of test %s#%s()", (i + 1),
					ClassUtils.getShortName(this.testMethod.getDeclaringClass()), this.testMethod.getName()));
			}
			this.next.evaluate();
		}
	}

}

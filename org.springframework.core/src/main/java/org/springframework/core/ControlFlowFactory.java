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

package org.springframework.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.util.Assert;

/**
 * Static factory to conceal the automatic choice of the ControlFlow
 * implementation class.
 *
 * <p>This implementation always uses the efficient Java 1.4 StackTraceElement
 * mechanism for analyzing control flows.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 02.02.2004
 */
public abstract class ControlFlowFactory {

	/**
	 * Return an appropriate {@link ControlFlow} instance.
	 */
	public static ControlFlow createControlFlow() {
		return new Jdk14ControlFlow();
	}


	/**
	 * Utilities for cflow-style pointcuts. Note that such pointcuts are
	 * 5-10 times more expensive to evaluate than other pointcuts, as they require
	 * analysis of the stack trace (through constructing a new throwable).
	 * However, they are useful in some cases.
	 * <p>This implementation uses the StackTraceElement class introduced in Java 1.4.
	 * @see java.lang.StackTraceElement
	 */
	static class Jdk14ControlFlow implements ControlFlow {

		private StackTraceElement[] stack;

		public Jdk14ControlFlow() {
			this.stack = new Throwable().getStackTrace();
		}

		/**
		 * Searches for class name match in a StackTraceElement.
		 */
		public boolean under(Class clazz) {
			Assert.notNull(clazz, "Class must not be null");
			String className = clazz.getName();
			for (int i = 0; i < stack.length; i++) {
				if (this.stack[i].getClassName().equals(className)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Searches for class name match plus method name match
		 * in a StackTraceElement.
		 */
		public boolean under(Class clazz, String methodName) {
			Assert.notNull(clazz, "Class must not be null");
			Assert.notNull(methodName, "Method name must not be null");
			String className = clazz.getName();
			for (int i = 0; i < this.stack.length; i++) {
				if (this.stack[i].getClassName().equals(className) &&
						this.stack[i].getMethodName().equals(methodName)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Leave it up to the caller to decide what matches.
		 * Caller must understand stack trace format, so there's less abstraction.
		 */
		public boolean underToken(String token) {
			if (token == null) {
				return false;
			}
			StringWriter sw = new StringWriter();
			new Throwable().printStackTrace(new PrintWriter(sw));
			String stackTrace = sw.toString();
			return stackTrace.indexOf(token) != -1;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Jdk14ControlFlow: ");
			for (int i = 0; i < this.stack.length; i++) {
				if (i > 0) {
					sb.append("\n\t@");
				}
				sb.append(this.stack[i]);
			}
			return sb.toString();
		}
	}

}

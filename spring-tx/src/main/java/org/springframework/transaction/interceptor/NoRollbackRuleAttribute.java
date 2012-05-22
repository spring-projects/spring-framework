/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.interceptor;

/**
 * Tag subclass of {@link RollbackRuleAttribute} that has the opposite behavior
 * to the <code>RollbackRuleAttribute</code> superclass.
 *
 * @author Rod Johnson
 * @since 09.04.2003
 */
public class NoRollbackRuleAttribute extends RollbackRuleAttribute {

	/**
	 * Create a new instance of the <code>NoRollbackRuleAttribute</code> class
	 * for the supplied {@link Throwable} class.
	 * @param clazz the <code>Throwable</code> class
	 * @see RollbackRuleAttribute#RollbackRuleAttribute(Class)
	 */
	public NoRollbackRuleAttribute(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * Create a new instance of the <code>NoRollbackRuleAttribute</code> class
	 * for the supplied <code>exceptionName</code>.
	 * @param exceptionName the exception name pattern
	 * @see RollbackRuleAttribute#RollbackRuleAttribute(String)
	 */
	public NoRollbackRuleAttribute(String exceptionName) {
		super(exceptionName);
	}

	@Override
	public String toString() {
		return "No" + super.toString();
	}

}

/*
 * Copyright 2002-2006 the original author or authors.
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

import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Transaction attribute that takes the EJB approach to rolling
 * back on runtime, but not checked, exceptions.
 *
 * @author Rod Johnson
 * @since 16.03.2003
 */
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

	/**
	 * Create a new DefaultTransactionAttribute, with default settings.
	 * Can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionAttribute() {
		super();
	}

	/**
	 * Copy constructor. Definition can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionAttribute(TransactionAttribute other) {
		super(other);
	}

	/**
	 * Create a new DefaultTransactionAttribute with the the given
	 * propagation behavior. Can be modified through bean property setters.
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}


	/**
	 * Default behavior is as with EJB: rollback on unchecked exception.
	 * Additionally attempt to rollback on Error.
	 * Consistent with TransactionTemplate's behavior.
	 */
	public boolean rollbackOn(Throwable ex) {
		return (ex instanceof RuntimeException || ex instanceof Error);
	}

}

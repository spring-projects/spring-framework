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

package org.springframework.transaction.support;

/**
 * A simple {@link org.springframework.transaction.TransactionStatus}
 * implementation.
 * 
 * <p>Derives from {@link AbstractTransactionStatus} and adds an explicit
 * {@link #isNewTransaction() "newTransaction"} flag.
 *
 * <p>This class is not used by any of Spring's pre-built
 * {@link org.springframework.transaction.PlatformTransactionManager}
 * implementations. It is mainly provided as a start for custom transaction
 * manager implementations and as a static mock for testing transactional
 * code (either as part of a mock <code>PlatformTransactionManager</code> or
 * as argument passed into a {@link TransactionCallback} to be tested).
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #SimpleTransactionStatus(boolean)
 * @see TransactionCallback
 */
public class SimpleTransactionStatus extends AbstractTransactionStatus {

	private final boolean newTransaction;


	/**
	 * Create a new instance of the {@link SimpleTransactionStatus} class,
	 * indicating a new transaction.
	 */
	public SimpleTransactionStatus() {
		this(true);
	}

	/**
	 * Create a new instance of the {@link SimpleTransactionStatus} class.
	 * @param newTransaction whether to indicate a new transaction
	 */
	public SimpleTransactionStatus(boolean newTransaction) {
		this.newTransaction = newTransaction;
	}


	public boolean isNewTransaction() {
		return this.newTransaction;
	}

}

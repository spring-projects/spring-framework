/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

/**
 * Enumeration that represents transaction propagation behaviors for use
 * with the {@link Transactional} annotation, corresponding to the
 * {@link TransactionDefinition} interface.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 */
public enum Propagation {

	/**
	 * Support a current transaction, create a new one if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p>This is the default setting of a transaction annotation.
	 * 支持当前事务，如果当前不存在事务，则新建一个事务。
	 */
	REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

	/**
	 *
	 * Support a current transaction: 表示如果当前存在一个事务（transaction），则该操作将参与到这个已存在的事务中。这通常用于在一个已经开启的事务中执行一系列操作。
	 * Execute non-transactionally if none exists: 表示如果当前没有已存在的事务，那么执行这个操作时不需要事务。这意味着该操作将在没有事务的情况下执行，不受事务的隔离和回滚等特性的影响。
	 * 综合起来，这个描述的含义是，该操作具备两种执行方式：
	 * 如果当前已经存在一个事务，那么该操作将参与到这个已存在的事务中，成为这个事务的一部分。
	 * 如果当前没有已存在的事务，那么该操作将在没有事务的情况下执行，不受事务的控制。
	 * <p>
	 * Support a current transaction, execute non-transactionally if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p>Note: For transaction managers with transaction synchronization,
	 * {@code SUPPORTS} is slightly different from no transaction at all,
	 * as it defines a transaction scope that synchronization will apply for.
	 * As a consequence, the same resources (JDBC Connection, Hibernate Session, etc)
	 * will be shared for the entire specified scope. Note that this depends on
	 * the actual synchronization configuration of the transaction manager.
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 */
	SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

	/**
	 * Support a current transaction, throw an exception if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * 必须在其他的事务内运行，如果当前不存在事务，则抛出异常
	 */
	MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

	/**
	 * 创建一个新事务，如果当前已存在事务，则将当前事务挂起。如果当前不存在事务，则行为和{@code REQUIRED}相同
	 * Create a new transaction, and suspend the current transaction if one exists.
	 * Analogous to the EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
	 * on all transaction managers. This in particular applies to
	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
	 * which requires the {@code jakarta.transaction.TransactionManager} to be
	 * made available to it (which is server-specific in standard Jakarta EE).
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

	/**
	 * 如果当前已经存在事务，则将当前事务挂起，该操作将在没有事务的情况下执行，不受事务的控制。
	 * Execute non-transactionally, suspend the current transaction if one exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
	 * on all transaction managers. This in particular applies to
	 * {@link org.springframework.transaction.jta.JtaTransactionManager},
	 * which requires the {@code jakarta.transaction.TransactionManager} to be
	 * made available to it (which is server-specific in standard Jakarta EE).
	 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
	 */
	NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

	/**
	 * Execute non-transactionally, throw an exception if a transaction exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p>
	 * 该操作以非事务的形式执行，如果当前存在事务，则抛出异常。
	 */
	NEVER(TransactionDefinition.PROPAGATION_NEVER),

	/**
	 * Execute within a nested transaction if a current transaction exists,
	 * behave like {@code REQUIRED} otherwise. There is no analogous feature in EJB.
	 * <p>Note: Actual creation of a nested transaction will only work on specific
	 * transaction managers. Out of the box, this only applies to the JDBC
	 * DataSourceTransactionManager. Some JTA providers might support nested
	 * transactions as well.
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	NESTED(TransactionDefinition.PROPAGATION_NESTED);


	private final int value;


	Propagation(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

}

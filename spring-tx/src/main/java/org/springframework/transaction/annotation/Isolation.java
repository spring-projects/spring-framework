/*
 * Copyright 2002-2023 the original author or authors.
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
 * 数据库系统必须具有隔离并发运行各个事务的能力，使它们不会相互影响，避免各种并发问题。一个事务与其他事务隔离的程度称为隔离级别。
 * SQL标准中规定了多种事务隔离级别，不同隔离级别对应不同的干扰程度，隔离级别越高，数据一致性就越好，但并发性越弱。
 * Enumeration that represents transaction isolation levels for use with the
 * {@link Transactional @Transactional} annotation, corresponding to the
 * {@link TransactionDefinition} interface.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 */
public enum Isolation {

	/**
	 * Use the default isolation level of the underlying data store.
	 * <p>All other levels correspond to the JDBC isolation levels.
	 * @see java.sql.Connection
	 */
	DEFAULT(TransactionDefinition.ISOLATION_DEFAULT),

	/**
	 * 允许 Transaction-1 读取 Transaction-2 未提交的修改
	 *
	 * A constant indicating that dirty reads, non-repeatable reads, and phantom reads
	 * can occur.
	 * <p>This level allows a row changed by one transaction to be read by
	 * another transaction before any changes in that row have been committed
	 * (a "dirty read"). If any of the changes are rolled back, the second
	 * transaction will have retrieved an invalid row.
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 */
	READ_UNCOMMITTED(TransactionDefinition.ISOLATION_READ_UNCOMMITTED),

	/**
	 * 要求 Transaction-1 只能读取 Transaction-2 已提交的修改
	 *
	 * A constant indicating that dirty reads are prevented; non-repeatable reads
	 * and phantom reads can occur.
	 * <p>This level only prohibits a transaction from reading a row with uncommitted
	 * changes in it.
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 */
	READ_COMMITTED(TransactionDefinition.ISOLATION_READ_COMMITTED),

	/**
	 * 确保 Transaction-1 可以多次从一个字段中读取到相同的值，即 Transaction-1 执行期间禁止其它事务对这个字段进行更新。
	 *
	 * A constant indicating that dirty reads and non-repeatable reads are
	 * prevented; phantom reads can occur.
	 * <p>This level prohibits a transaction from reading a row with uncommitted changes
	 * in it, and it also prohibits the situation where one transaction reads a row,
	 * a second transaction alters the row, and the first transaction re-reads the row,
	 * getting different values the second time (a "non-repeatable read").
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 */
	REPEATABLE_READ(TransactionDefinition.ISOLATION_REPEATABLE_READ),

	/**
	 * 确保 Transaction-1 可以多次从一个表中读取到相同的行，在 Transaction-1 执行期间，禁止其它事务对这个表进行添加、更新、删除操作。可以避免任何并发问题，但性能十分低下。
	 *
	 * A constant indicating that dirty reads, non-repeatable reads, and phantom
	 * reads are prevented.
	 * <p>This level includes the prohibitions in {@link #REPEATABLE_READ}
	 * and further prohibits the situation where one transaction reads all rows that
	 * satisfy a {@code WHERE} condition, a second transaction inserts a row
	 * that satisfies that {@code WHERE} condition, and the first transaction
	 * re-reads for the same condition, retrieving the additional "phantom" row
	 * in the second read.
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 */
	SERIALIZABLE(TransactionDefinition.ISOLATION_SERIALIZABLE);


	private final int value;


	Isolation(int value) {
		this.value = value;
	}

	public int value() {
		return this.value;
	}

}

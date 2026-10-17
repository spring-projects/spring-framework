/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import java.sql.Connection;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link HibernateTransactionManager}.
 *
 * @author Sam Brannen
 * @since 7.1
 */
class HibernateTransactionManagerTests {

	private final DataSource dataSource = mock();


	@AfterEach
	void unbindDataSource() {
		if (TransactionSynchronizationManager.hasResource(this.dataSource)) {
			TransactionSynchronizationManager.unbindResource(this.dataSource);
		}
	}


	@Test  // gh-37356
	void transactionWithinOtherTransactionManagerForSameDataSource() {
		HibernateTransactionManager tm = new HibernateTransactionManager(mock(SessionFactory.class));
		tm.setAutodetectDataSource(false);
		tm.setDataSource(this.dataSource);
		TransactionTemplate transactionTemplate = new TransactionTemplate(tm);

		// Simulate a JDBC Connection exposed by another transaction manager for the same DataSource.
		TransactionSynchronizationManager.bindResource(this.dataSource, new ConnectionHolder(mock(Connection.class)));

		assertThatExceptionOfType(IllegalTransactionStateException.class)
				.isThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {}))
				.withMessageStartingWith("Pre-bound JDBC Connection found!")
				.withMessageContaining("another transaction manager")
				.withMessageNotContaining("DataSourceTransactionManager");
	}

}

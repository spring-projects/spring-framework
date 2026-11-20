/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.InfrastructureProxy;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link DataSourceUtils}.
 *
 * @author Kevin Schoenfeld
 * @author Stephane Nicoll
 */
class DataSourceUtilsTests {

	@Test
	void connectionNotAcquiredExceptionIsPropagated() throws SQLException {
		DataSource dataSource = mock();
		when(dataSource.getConnection()).thenReturn(null);
		assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
				.isInstanceOf(CannotGetJdbcConnectionException.class)
				.hasMessageStartingWith("Failed to obtain JDBC Connection")
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void connectionSQLExceptionIsPropagated() throws SQLException {
		DataSource dataSource = mock();
		when(dataSource.getConnection()).thenThrow(new SQLException("my dummy exception"));
		assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
				.isInstanceOf(CannotGetJdbcConnectionException.class)
				.hasMessageStartingWith("Failed to obtain JDBC Connection")
				.cause().isInstanceOf(SQLException.class)
				.hasMessage("my dummy exception");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void transactionCleanupUsesOriginalResourceKey(boolean releaseBeforeCompletion) throws SQLException {
		DataSource target = mock();
		DataSource fallback = mock();
		DataSource first = mock(DataSource.class, withSettings().extraInterfaces(InfrastructureProxy.class));
		DataSource second = mock(DataSource.class, withSettings().extraInterfaces(InfrastructureProxy.class));
		Connection connection = mock();
		when(first.getConnection()).thenReturn(connection);
		when(((InfrastructureProxy) first).getWrappedObject()).thenReturn(target);
		when(((InfrastructureProxy) second).getWrappedObject()).thenReturn(target);

		TransactionSynchronizationManager.initSynchronization();
		try {
			assertThat(DataSourceUtils.getConnection(first)).isSameAs(connection);
			assertThat(DataSourceUtils.getConnection(second)).isSameAs(connection);
			DataSourceUtils.releaseConnection(connection, second);
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			assertThat(synchronizations).hasSize(1);
			TransactionSynchronization synchronization = synchronizations.get(0);

			if (releaseBeforeCompletion) {
				DataSourceUtils.releaseConnection(connection, first);
				when(((InfrastructureProxy) first).getWrappedObject()).thenReturn(fallback);
			}
			synchronization.beforeCompletion();
			ConnectionHolder otherHolder = new ConnectionHolder(mock(Connection.class));
			TransactionSynchronizationManager.bindResource(fallback, otherHolder);
			when(((InfrastructureProxy) first).getWrappedObject()).thenReturn(fallback);
			synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

			assertThat(TransactionSynchronizationManager.getResource(target)).isNull();
			assertThat(TransactionSynchronizationManager.getResource(fallback)).isSameAs(otherHolder);
			verify(connection).close();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
			TransactionSynchronizationManager.unbindResourceIfPossible(target);
			TransactionSynchronizationManager.unbindResourceIfPossible(fallback);
		}
	}

}


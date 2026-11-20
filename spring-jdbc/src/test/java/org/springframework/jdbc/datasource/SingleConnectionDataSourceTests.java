/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 6.1.2
 */
class SingleConnectionDataSourceTests {

	private final Connection connection = mock();


	@Test
	void plainConnection() throws Exception {
		SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, false);

		ds.getConnection().close();
		verify(connection, times(1)).close();
	}

	@Test
	void withAutoCloseable() throws Exception {
		try (SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, false)) {
			ds.getConnection();
		}

		verify(connection, times(1)).close();
	}

	@Test
	void withSuppressClose() throws Exception {
		SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);

		ds.getConnection().close();
		verify(connection, never()).close();

		ds.destroy();
		verify(connection, times(1)).close();

		given(connection.isClosed()).willReturn(true);
		assertThatExceptionOfType(SQLException.class).isThrownBy(ds::getConnection);
	}

	@Test
	void withRollbackBeforeClose() throws Exception {
		SingleConnectionDataSource ds = new SingleConnectionDataSource(connection, true);
		ds.setRollbackBeforeClose(true);

		ds.destroy();
		verify(connection, times(1)).rollback();
		verify(connection, times(1)).close();
	}

	@Test
	void withEnforcedAutoCommit() throws Exception {
		SingleConnectionDataSource ds = new SingleConnectionDataSource() {
			@Override
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				return connection;
			}
		};
		ds.setUrl("url");
		ds.setAutoCommit(true);

		ds.getConnection();
		verify(connection, times(1)).setAutoCommit(true);

		ds.destroy();
		verify(connection, times(1)).close();
	}

}

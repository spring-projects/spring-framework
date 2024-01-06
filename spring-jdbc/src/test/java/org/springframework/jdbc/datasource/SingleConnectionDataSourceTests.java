/*
 * Copyright 2002-2024 the original author or authors.
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

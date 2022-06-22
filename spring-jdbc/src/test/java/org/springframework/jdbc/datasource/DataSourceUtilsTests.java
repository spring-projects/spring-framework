/*
 * Copyright 2002-2022 the original author or authors.
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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;

/**
 * @author Kevin Schoenfeld
 * @see org.springframework.jdbc.datasource.DataSourceUtilsTests
 * @since 21.06.2022
 */

class DataSourceUtilsTests {

	@Test
	void testConnectionNotAcquiredExceptionIsPropagated() throws SQLException {
		final DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(null);

		assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
				.isInstanceOf(CannotGetJdbcConnectionException.class)
				.hasMessageStartingWith("Failed to obtain JDBC Connection")
				.hasCauseInstanceOf(IllegalStateException.class);
	}

	@Test
	void testConnectionSQLExceptionIsPropagated() throws SQLException {
		final DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenThrow(new SQLException("my dummy exception"));

		assertThatThrownBy(() -> DataSourceUtils.getConnection(dataSource))
				.isInstanceOf(CannotGetJdbcConnectionException.class)
				.hasMessageStartingWith("Failed to obtain JDBC Connection")
				.hasCauseInstanceOf(SQLException.class);
	}
}

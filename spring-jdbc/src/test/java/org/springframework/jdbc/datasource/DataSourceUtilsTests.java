/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

}


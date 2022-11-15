/*
 * Copyright 2003-2020 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for {@code DatabaseStartupValidator}.
 *
 * @author Marten Deinum
 */
class DatabaseStartupValidatorTests {

	private final DataSource dataSource = mock(DataSource.class);

	private final Connection connection = mock(Connection.class);

	private final DatabaseStartupValidator validator = new DatabaseStartupValidator();


	@BeforeEach
	void setUp() throws Exception {
		given(dataSource.getConnection()).willReturn(connection);
		validator.setDataSource(dataSource);
		validator.setTimeout(3); // ensure tests don't accidentally run too long
	}

	@Test
	void exceededTimeoutThrowsException() {
		validator.setTimeout(1);
		assertThatExceptionOfType(CannotGetJdbcConnectionException.class)
			.isThrownBy(validator::afterPropertiesSet);
	}

	@Test
	void properSetupForDataSource() {
		validator.setDataSource(null);

		assertThatIllegalArgumentException().isThrownBy(validator::afterPropertiesSet);
	}

	@Test
	void shouldUseJdbc4IsValidByDefault() throws Exception {
		given(connection.isValid(1)).willReturn(true);

		validator.afterPropertiesSet();

		verify(connection, times(1)).isValid(1);
		verify(connection, times(1)).close();
	}

	@Test
	void shouldCallValidationTwiceWhenNotValid() throws Exception {
		given(connection.isValid(1)).willReturn(false, true);

		validator.afterPropertiesSet();

		verify(connection, times(2)).isValid(1);
		verify(connection, times(2)).close();
	}

	@Test
	void shouldCallValidationTwiceInCaseOfException() throws Exception {
		given(connection.isValid(1)).willThrow(new SQLException("Test")).willReturn(true);

		validator.afterPropertiesSet();

		verify(connection, times(2)).isValid(1);
		verify(connection, times(2)).close();
	}

	@Test
	@SuppressWarnings("deprecation")
	void useValidationQueryInsteadOfIsValid() throws Exception {
		String validationQuery = "SELECT NOW() FROM DUAL";
		Statement statement = mock(Statement.class);
		given(connection.createStatement()).willReturn(statement);
		given(statement.execute(validationQuery)).willReturn(true);

		validator.setValidationQuery(validationQuery);
		validator.afterPropertiesSet();

		verify(connection, times(1)).createStatement();
		verify(statement, times(1)).execute(validationQuery);
		verify(connection, times(1)).close();
		verify(statement, times(1)).close();
	}

	@Test
	@SuppressWarnings("deprecation")
	void shouldExecuteValidationTwiceOnError() throws Exception {
		String validationQuery = "SELECT NOW() FROM DUAL";
		Statement statement = mock(Statement.class);
		given(connection.createStatement()).willReturn(statement);
		given(statement.execute(validationQuery))
				.willThrow(new SQLException("Test"))
				.willReturn(true);

		validator.setValidationQuery(validationQuery);
		validator.afterPropertiesSet();

		verify(connection, times(2)).createStatement();
		verify(statement, times(2)).execute(validationQuery);
		verify(connection, times(2)).close();
		verify(statement, times(2)).close();
	}

}

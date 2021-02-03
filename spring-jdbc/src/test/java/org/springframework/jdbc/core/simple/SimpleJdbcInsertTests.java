/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for {@link SimpleJdbcInsert}.
 *
 * @author Thomas Risberg
 */
class SimpleJdbcInsertTests {

	private final Connection connection = mock(Connection.class);

	private final DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);

	private final DataSource dataSource = mock(DataSource.class);


	@BeforeEach
	void setUp() throws Exception {
		given(connection.getMetaData()).willReturn(databaseMetaData);
		given(dataSource.getConnection()).willReturn(connection);
	}

	@AfterEach
	void verifyClosed() throws Exception {
		verify(connection).close();
	}


	@Test
	void noSuchTable() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.next()).willReturn(false);

		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getDatabaseProductVersion()).willReturn("1.0");
		given(databaseMetaData.getUserName()).willReturn("me");
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(databaseMetaData.getTables(null, null, "x", null)).willReturn(resultSet);

		SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("x");
		// Shouldn't succeed in inserting into table which doesn't exist
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
			.isThrownBy(() -> insert.execute(Collections.emptyMap()))
			.withMessageStartingWith("Unable to locate columns for table 'x' so an insert statement can't be generated");

		verify(resultSet).close();
	}

}

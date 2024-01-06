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

package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 02.08.2004
 */
class RowMapperTests {

	private final Connection connection = mock();

	private final Statement statement = mock();

	private final PreparedStatement preparedStatement = mock();

	private final ResultSet resultSet = mock();

	private final JdbcTemplate template = new JdbcTemplate();

	private final RowMapper<TestBean> testRowMapper =
			(rs, rowNum) -> new TestBean(rs.getString(1), rs.getInt(2));

	private List<TestBean> result;

	@BeforeEach
	void setUp() throws SQLException {
		given(connection.createStatement()).willReturn(statement);
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
		given(statement.executeQuery(anyString())).willReturn(resultSet);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getString(1)).willReturn("tb1", "tb2");
		given(resultSet.getInt(2)).willReturn(1, 2);

		template.setDataSource(new SingleConnectionDataSource(connection, false));
		template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		template.afterPropertiesSet();
	}

	@AfterEach
	void verifyClosed() throws Exception {
		verify(resultSet).close();
	}

	@AfterEach
	void verifyResults() {
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		TestBean testBean1 = result.get(0);
		TestBean testBean2 = result.get(1);
		assertThat(testBean1.getName()).isEqualTo("tb1");
		assertThat(testBean2.getName()).isEqualTo("tb2");
		assertThat(testBean1.getAge()).isEqualTo(1);
		assertThat(testBean2.getAge()).isEqualTo(2);
	}

	@Test
	void staticQueryWithRowMapper() throws SQLException {
		result = template.query("some SQL", testRowMapper);
		verify(statement).close();
	}

	@Test
	void preparedStatementCreatorWithRowMapper() throws SQLException {
		result = template.query(con -> preparedStatement, testRowMapper);
		verify(preparedStatement).close();
	}

	@Test
	void preparedStatementSetterWithRowMapper() throws SQLException {
		result = template.query("some SQL", ps -> ps.setString(1, "test"), testRowMapper);
		verify(preparedStatement).setString(1, "test");
		verify(preparedStatement).close();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void queryWithArgsAndRowMapper() throws SQLException {
		result = template.query("some SQL", new Object[] { "test1", "test2" }, testRowMapper);
		preparedStatement.setString(1, "test1");
		preparedStatement.setString(2, "test2");
		preparedStatement.close();
	}

	@Test
	void queryWithArgsAndTypesAndRowMapper() throws SQLException {
		result = template.query("some SQL",
				new Object[] { "test1", "test2" },
				new int[] { Types.VARCHAR, Types.VARCHAR },
				testRowMapper);
		verify(preparedStatement).setString(1, "test1");
		verify(preparedStatement).setString(2, "test2");
		verify(preparedStatement).close();
	}

}
